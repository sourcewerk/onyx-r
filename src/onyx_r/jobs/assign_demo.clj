;; onyx-r
;; Copyright (c) 2016 sourcewerk GmbH.
;; Distributed under the Eclipse Public License, see LICENSE for details. 

(ns onyx-r.jobs.assign-demo
  (:require [onyx.job :refer [add-task]]
            [onyx.tasks.core-async :as core-async-task]
            [onyx-r.tasks.r :as r]))


(defn assign-demo-job
  [batch-settings]
  (let [base-job   {:workflow        [[:in :rfun]
                                      [:rfun :out]]
                    :catalog         []
                    :lifecycles      []
                    :windows         []
                    :triggers        []
                    :flow-conditions []
                    :task-scheduler  :onyx.task-scheduler/balanced}]
    (-> base-job
        (add-task (core-async-task/input :in batch-settings))
        (add-task (r/r-function :rfun
                                "rfun"
                                {:source ["rfun <- function(segment) list(n = segment$n +rVar1 * rVar2)"]
                                 :assign {:rVar1 1.0
                                          :rVar2 2.0}}
                                batch-settings))
        (add-task (core-async-task/output :out batch-settings)))))

