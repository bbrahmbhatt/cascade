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

(ns cascade.test-utils
  (:use
    (clojure (test :exclude [function?]))
    (clojure.contrib pprint duck-streams str-utils macro-utils)
    cascade.config
    (cascade func-utils utils)
    (cascade.internal utils)))

(deftest classpath-resource-does-not-exist
  (is (= (find-classpath-resource "does/not/exist.txt") nil)))

(deftest classpath-resource-exists
  (let [path "cascade/internal/resource.txt"]
    (is (= (slurp* (find-classpath-resource path))
      (slurp (str "src/test/resources/" path))))))

(deftest namespace-relative
  (is (= (slurp* (find-classpath-resource 'namespace-relative "ns.txt"))
    (slurp "src/test/resources/cascade/test_utils/ns.txt"))))

(deftest test-function?
  (is (= (function? map) true) "a real function")
  (is (= (function? nil) false) "nil is not a function")
  (is (= (function? "string") false) "strings are not functions")
  (is (= (function? {}) true) "maps act as a function"))

(deftest test-to-seq
  (let [v [:a :b]]
    (is (identical? (to-seq v) v)))
  (is (= (to-seq :a) [:a])))

(deftest test-qualified-function-name
  (is (= (qualified-function-name #'map) "clojure.core/map"))
  (is (= (qualified-function-name #'function?) "cascade.internal.utils/function?")))

(deftest test-lcond
  (let [f #(lcond (nil? %) "nil"
                  :let [x2 (* 2 %)]
                  true x2)]
    (is (= (f nil) "nil"))
    (is (= (f 5) 10))))

(deftest empty-lcond-is-nil
  (is (nil? (lcond))))

(deftest lcond-requires-even-clauses
  (is (thrown-with-msg? RuntimeException #".* lcond requires an even number of forms"
    (mexpand-all `(lcond (= 1 2) :b :c)))))
    
(deftest test-boolean?
  (are [val expected]
    (is (= (boolean? val) expected))
    true true
    Boolean/TRUE true
    false true
    Boolean/FALSE true
    nil false
    "whatever" false
    0 false
    1 false))    