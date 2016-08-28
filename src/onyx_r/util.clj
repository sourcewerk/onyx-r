;; onyx-r
;; Copyright (c) 2016 sourcewerk GmbH.
;; Distributed under the Eclipse Public License, see LICENSE for details. 

(ns onyx-r.util
  (:import 
    (org.rosuda.REngine REXPDouble REXPGenericVector REXPInteger REXPLogical REXPNull REXPRaw REXPString REXPSymbol REXPWrapper RList)
    (org.rosuda.REngine.Rserve RConnection)))


;; IO utilities...
(defn slurp-bytes
  "Slurp the bytes from a slurpable thing"
  [x]
  (with-open [out (java.io.ByteArrayOutputStream.)]
    (clojure.java.io/copy (clojure.java.io/input-stream x) out)
    (.toByteArray out)))

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

