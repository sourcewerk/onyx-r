;; onyx-r
;; Copyright (c) 2016 sourcewerk GmbH.
;; Distributed under the Eclipse Public License, see LICENSE for details. 

(ns onyx-r.tasks.r
  (:require
   [schema.core :as s])
  (:import 
    (org.rosuda.REngine REXPDouble REXPGenericVector REXPInteger REXPLogical REXPNull REXPRaw REXPString REXPSymbol REXPWrapper RList)
    (org.rosuda.REngine.Rserve RConnection)))


;; R data structure conversion...
(defn clojure-data->r-data
  "Tries to convert a Clojure data structure an equivalent R data structure."
  [data]
  (let [wrapped-data (if (nil? data) nil (. REXPWrapper wrap data))] ; wrap crashes on NULL
    (if (nil? wrapped-data)
      (cond
        ;; base cases...
        (nil? data) (new REXPNull)
        (keyword? data) (new REXPSymbol (name data))
        (symbol? data) (new REXPSymbol (name data))
        ;; recursive cases...
        (vector? data) (new REXPGenericVector (new RList (map clojure-data->r-data data)))
        (set? data) (new REXPGenericVector (new RList (map clojure-data->r-data data)))
        (map? data) (new REXPGenericVector (new RList (map clojure-data->r-data (vals data)) ; values
                                                      (map #(if (keyword? %) (name %) (str %))
                                                           (keys data)))) ; keys as strings (R limitation)
        ;; not supported
        :else (throw (new Exception (str "clojure-data->r-data: unsupported data '" data "' of class '" (class data) "'"))))
      wrapped-data)))

(defn r-data->clojure-data
  "Tries to convert a R data structure to an equivalent Clojure data structure."
  [data]
  (cond
    ;; base cases...
    (instance? REXPNull data) nil
    (instance? REXPSymbol data) (symbol (.asString data))
    (instance? REXPDouble data) (if (= 1 (.length data)) (.asDouble data) (vec (.asDoubles data)))
    (instance? REXPInteger data) (if (= 1 (.length data)) (.asInteger data) (vec (.asIntegers data))) 
    (instance? REXPLogical data) (if (= 1 (.length data)) (get (.isTRUE data) 0) (vec (.isTRUE data)))
    (instance? REXPRaw data) (.asBytes data)
    (instance? REXPString data) (if (= 1 (.length data)) (.asString data) (vec (.asStrings data))) 
    ;; recursive case...
    (instance? REXPGenericVector data) (let [data-list (.asList data)]
                                         (if (.isNamed data-list)
                                           (apply array-map (interleave (map keyword (.keys data-list))
                                                                        (map r-data->clojure-data (.values data-list))))
                                           (vec (map r-data->clojure-data data-list))))
    ;; not supported
    :else (throw (new Exception (str "r-data->clojure-data: unsupported data '" data "' of class '" (class data) "'")))))

;; R lifecycle calls...
(defn before-r-task-start [event lifecycle]
  (let [r-connection (new org.rosuda.REngine.Rserve.RConnection)
        task-map (:onyx.core/task-map event)
        convert-result-to-clojure-data (::convert-result-to-clojure-data task-map)
        source (::source task-map)
        load (::load task-map)
        assign (::assign task-map)]
    ;; 1) source strings into R session...
    (doseq [source-string source]
      (try
        (with-open [r-file-writer (clojure.java.io/writer (.createFile r-connection "onyx-r_source.R"))]
          (.write r-file-writer source-string))
        (.voidEval r-connection "source('onyx-r_source.R')")
        (finally (.removeFile r-connection "onyx-r_source.R"))))
    ;; 2) load RData byte arrays into R session...
    (doseq [load-array load]
      (try
        (with-open [r-file-output-stream (.createFile r-connection "onyx-r_load.RData")]
          (.write r-file-output-stream load-array))
        (.voidEval r-connection "load('onyx-r_load.RData')")
        (finally (.removeFile r-connection "onyx-r_load.RData"))))
    ;; 3) assign variables in R session...
    (doseq [[k v] assign]
      (.assign r-connection (name k) (clojure-data->r-data v)))
    ;; 4) finally, bind r-connection to r-fn parameter 0 and convert-result-to-clojure-data to r-fn parameter 2
    {:onyx.core/params (-> (:onyx.core/params event)
                           (assoc 0 r-connection)
                           (assoc 2 (if (nil? convert-result-to-clojure-data) true convert-result-to-clojure-data)))}))

(defn after-r-task-stop [event lifecycle]
  (let [r-connection (get (:onyx.core/params event) 0)]
    (.close r-connection)
    {}))

(def r-lifecycle-calls {:lifecycle/before-task-start before-r-task-start
                        :lifecycle/after-task-stop after-r-task-stop})

;; R task function...
(defn byte-array?
  "Checks wether o is a primitive Java byte array."
  [o]
  (let [c (class o)]
    (and (.isArray c)
         (identical? (.getComponentType c) Byte/TYPE))))

(defn r-fn [r-connection call convert-result-to-clojure-data segment]
  (let [r-result (do (.assign r-connection "segment" (clojure-data->r-data segment))
                     (.eval r-connection (str call "(segment)"))) ; call "call" function on segment 
        result (if convert-result-to-clojure-data
                 (r-data->clojure-data r-result) ; convert entire segment map to clojure data structures
                 (into {} (.asNativeJavaObject r-result)))] ; convert only outer segment map to clojure map
    result))

(s/defn r-function
  ([task-name :- s/Keyword
    task-opts]
   {:task {:task-map (merge {:onyx/name task-name
                             :onyx/type :function
                             :onyx/fn ::r-fn
                             :onyx/params [::r-connection ::call ::convert-result-to-clojure-data]}
                            task-opts)
           :lifecycles [{:lifecycle/task task-name 
                         :lifecycle/calls ::r-lifecycle-calls
                         :lifecycle/doc "Initialize R environment on task start."}]} 
    :schema {:task-map {::call s/Str
                        (s/optional-key ::convert-result-to-clojure-data) (s/maybe s/Bool)
                        (s/optional-key ::source) (s/maybe [s/Str])
                        (s/optional-key ::load) (s/maybe [(s/pred byte-array?)])
                        (s/optional-key ::assign) (s/maybe {s/Keyword s/Any})}}})
  ([task-name :- s/Keyword
    call :- s/Str
    r-function-opts :- {(s/optional-key :convert-result-to-clojure-data) (s/maybe s/Bool)
                        (s/optional-key :source) (s/maybe [s/Str])
                        (s/optional-key :load) (s/maybe [(s/pred byte-array?)])
                        (s/optional-key :assign) (s/maybe {s/Keyword s/Any})}
    task-opts]
   (r-function task-name (merge {::call call
                                 ::convert-result-to-clojure-data (:convert-result-to-clojure-data r-function-opts)
                                 ::source (:source r-function-opts)
                                 ::load (:load r-function-opts)
                                 ::assign (:assign r-function-opts)}
                                task-opts))))

