# <img alt="Onyx Logo" src="https://i.imgur.com/zdlOSZD.png?1" height="64"> + <img alt="R Logo" src="https://www.r-project.org/Rlogo.png" height="64"> = onyx-r

*Onyx Task Bundle for Implementing Data Processing Tasks in R*

---


## Rationale

onyx-r provides an Onyx [task
bundle](http://www.onyxplatform.org/jekyll/update/2016/06/13/Task-Bundles.html)
for running data processing tasks in [R](https://www.r-project.org).

A typical use case is using R models (created via statistical or machine
learning algorithms) in Onyx job workflows, at scale.

Each Onyx peer runs an Rserve instance, each virtual peer holds a connection to
its local Rserve instance. onyx-r tasks are configured at job submit time
through pure Clojure data in the Onyx catalog. onyx-r tasks are implemented as
pure R functions that take an Onyx segment as input and return a modified Onyx
segment as output. For this to work seamlessly, onyx-r automatically translates
between Clojure and R data structures. onyx-r tasks must be configured with the
name of the R segment processing function to call.

When an onyx-r task is prepared for execution on a virtual peer through [Onyx
lifecycles](http://www.onyxplatform.org/docs/user-guide/0.9.10-beta1/#lifecycles),
the task can be provided with R code to `source`, R data (in `RData` format
exported from R via `save`) to `load` and Clojure values to assign to R
variables to `assign`. These configuration options are also supplied by the
user at job submit time through the Onyx catalog.


## Quick Start Guide

TODO


## Running the Tests

First, start a local Rserve server as documented at:
https://www.rforge.net/Rserve/doc.html#start

Then type `lein test` to runn all tests for onyx-r.


## License

Copyright © 2016 sourcewerk GmbH

Distributed under the Eclipse Public License, the same as Clojure and Onyx.


## Contact

Commercial support is available through sourcewerk GmbH:

https://sourcewerk.de
info@soucewerk.de

