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

(ns com.howardlewisship.cascade.test-utils
  (:use
    (clojure.contrib (test-is :only [is deftest]) pprint duck-streams)
    com.howardlewisship.cascade.config
    com.howardlewisship.cascade.internal.utils))

(deftest classpath-resource-does-not-exist
  (is (= (find-classpath-resource "does/not/exist.txt") nil)))

(deftest classpath-resource-exists
  (let [path "com/howardlewisship/cascade/internal/resource.txt"]
    (is (= (slurp* (find-classpath-resource path))
      (slurp (str "src/test/resources/" path))))))

(deftest namespace-relative
  (is (= (slurp* (find-classpath-resource 'namespace-relative "ns.txt"))
    (slurp "src/test/resources/com/howardlewisship/cascade/test_utils/ns.txt"))))
    
(deftest to-str-list-conversions
  (is (= (to-str-list nil) "(none)"))
  (is (= (to-str-list []) "(none)"))
  (is (= (to-str-list ["fred"]) "fred"))
  (is (= (to-str-list ["fred" "barney" "wilma"]) "fred, barney, wilma")))    


(defn- test-chain
  [chains selector expected]
  (binding [configuration { :chains chains}]
    (let [chain (create-chain selector)]
      (is (= (chain nil) expected)))))

(defn- always
  [result]
  (fn [_] result))

(deftest chain-to-function
  (test-chain { :test (always :goober) } :test :goober))
  
(deftest indirect-chain
  (test-chain { :test :indirect, :indirect (always :final) } :test :final))

(deftest nil-in-chain-ignored
  (test-chain { :test [ nil :indirect], :indirect (always :final) } :test :final))
  
(deftest sequence-of-functions-in-chain
  (test-chain { :test [ (always nil) (always :final)]} :test :final))
      
(deftest test-function?
  (is (= (function? map) true) "a real function")
  (is (= (function? nil) false) "nil is not a function")    
  (is (= (function? "string") false) "strings are not functions")
  (is (= (function? {}) true) "maps act as a function"))