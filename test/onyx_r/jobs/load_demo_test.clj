;; onyx-r
;; Copyright (c) 2016 sourcewerk GmbH.
;; Distributed under the Eclipse Public License, see LICENSE for details. 

(ns onyx-r.jobs.load-demo-test
  (:require [aero.core :refer [read-config]]
            [clojure.core.async :refer [>!!]]
            [clojure.java.io :as io]
            [clojure.test :refer [deftest is testing]]
            [onyx api [test-helper :refer [with-test-env]]]
            [onyx.plugin.core-async :refer [get-core-async-channels take-segments!]]
            onyx-r.jobs.load-demo
            ;; Include function definitions
            onyx-r.tasks.r
            onyx.tasks.core-async))


(def segments [{:n 1.0} {:n 2.0} {:n 3.0} {:n 4.0} {:n 5.0} :done])

(deftest load-demo-test
  (testing "that we can load R data through onyx-r"
    (let [{:keys [env-config
                  peer-config]} (read-config (io/resource "config.edn"))
          job (onyx-r.jobs.load-demo/load-demo-job {:onyx/batch-size 10
                                                    :onyx/batch-timeout 1000})
          {:keys [in out]} (get-core-async-channels job)]
      (with-test-env [test-env [3 env-config peer-config]]
        (onyx.test-helper/validate-enough-peers! test-env job)
        (onyx.api/submit-job peer-config job)
        (doseq [segment segments]
          (>!! in segment))
        (is (= (set (take-segments! out))
               (set [{:n [2.0 3.0 4.0 5.0 6.0 7.0 8.0 9.0 10.0 11.0]}
                     {:n [3.0 4.0 5.0 6.0 7.0 8.0 9.0 10.0 11.0 12.0]}
                     {:n [4.0 5.0 6.0 7.0 8.0 9.0 10.0 11.0 12.0 13.0]}
                     {:n [5.0 6.0 7.0 8.0 9.0 10.0 11.0 12.0 13.0 14.0]}
                     {:n [6.0 7.0 8.0 9.0 10.0 11.0 12.0 13.0 14.0 15.0]}
                     :done])))))))

