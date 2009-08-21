; Copyright 2009 Howard M. Lewis Ship
;
; Licensed under the Apache License, Version 2.0 (the "License");
; you may not use this file except in compliance with the License.
; You may obtain a copy of the License at
;
;   http://www.apache.org/licenses/LICENSE-2.0
;
; Unless required by applicable law or agreed to in writing, software
; distributed under the License is distributed on an "AS IS" BASIS,
; WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
; implied. See the License for the specific language governing permissions
; and limitations under the License.

(ns cascade
  (:use
    (cascade config path-map)
    (cascade.internal utils viewbuilder parse-functions)))
  
(defmacro inline
  "Defines a block of template that renders inline."
  [& template]
  (parse-embedded-template template))
    
(defmacro defview
  "Defines a Cascade view function, which uses an embedded template. A view function may have a doc string and meta data
  preceding the parameters vector. The function's forms are an implicit inline block."
  [& forms]
  (let [[fn-name fn-params template] (parse-function-def forms)
        full-meta (merge ^fn-name {:cascade-type :view})]
  `(add-mapped-function (defn ~fn-name ~full-meta ~fn-params (inline ~@template)))))

(defmacro block
  "Defines a block of template that renders with an environment controlled by its container. The result is a function
that takes a single parameter (the env map)."
  [fn-params & template]
  (fail-unless (and (vector? fn-params) (= 1 (count fn-params))) "Blocks require that exactly one parameter be defined.")
    `(fn ~fn-params (inline ~@template)))

(def #^{:doc "A DOM text node for a line break."}
  linebreak
  (text-node "\r"))
  
;; TODO: get away from defining chains in terms of a keyword, use a symbol  
  
(defmacro defchain
  "Defines a function as part of a chain. Chain functions take a single parameter. The name parameter is a keyword
  added to the :chains configuration map (this is to encourage chains to be composable by keyword)."
  [name fn-params & forms]
  (fail-unless (keyword? name) "A chain is identified by a keyword, not a symbol.")
  (fail-unless (vector? fn-params) "A chain function must define parameters like any other function.")
  `(assoc-in-config [:chains ~name] (fn ~fn-params ~@forms)))
  
(defmacro deffilter
  "Defines a filter function that can be used to assemble a pipeline. The name of the filter is a keyword (not
  a symbol). A filter function receives a delegate before its other parameters, it should invoke the delegate
  function, passing it appropriate parameters."
  [name fn-params & forms]
  (fail-unless (keyword? name) "A filter is identified by a keyword, not a symbol.")
  (fail-unless (vector? fn-params) "A filter function must define parameters like any other function.")
  (fail-unless (>= (count fn-params) 1) "A filter function must define at least one parameter (to recieve the delegate).")
  ; TODO: Is ":pipelines" the right name?  Should it be :filters or :pipeline-filters?  Oh, well.
  `(assoc-in-config [:filters ~name] (fn ~fn-params ~@forms)))
