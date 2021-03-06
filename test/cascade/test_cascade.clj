;;; Copyright 2009, 2010, 2011 Howard M. Lewis Ship
;;;
;;; Licensed under the Apache License, Version 2.0 (the "License");
;;; you may not use this file except in compliance with the License.
;;; You may obtain a copy of the License at
;;;
;;;   http://www.apache.org/licenses/LICENSE-2.0
;;;
;;; Unless required by applicable law or agreed to in writing, software
;;; distributed under the License is distributed on an "AS IS" BASIS,
;;; WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
;;; implied. See the License for the specific language governing permissions
;;; and limitations under the License.

(ns cascade.test-cascade
  (:import
   [java.io PrintWriter CharArrayWriter])
  (:use
   cascade
   [cascade dom]
   [clojure [test :only (is are deftest)] pprint]))

(defn response [body]
                                        ; Make sure the (response) invoked by the defview macro can't conflict with a method of the same
                                        ; name in the calling namespace.
  (throw (IllegalStateException. "This should never be called.")))


(defn serialize-to-string [dom]
                                        ; This is very inefficient as it realizes the entire seq of strings all at once, but good
                                        ; enough for testing.
  (apply str (serialize-html dom)))

(defn minimize-ws [^String string]
  (.replaceAll string "\\s+" " "))

(defn find-classpath-resource [path]
  (.. Thread currentThread getContextClassLoader (getResourceAsStream path)))

(defn serialize-test
  [view-fn name & rest]
  (let [input-path (str "expected/" name ".txt")
        expected (slurp (find-classpath-resource input-path))
        trimmed-expected (minimize-ws expected)
        response (apply view-fn rest)
        streamed (serialize-to-string (:body response))
        trimmed-actual (minimize-ws streamed)]
    (is (= trimmed-actual trimmed-expected))))

(defview ^{:custom :bit-of-meta-data} simple-view
  [req]
  [:p (req :message)])

(deftest simple-defview
  (serialize-test simple-view "simple-defview" {:message "Embedded Template"}))

(deftest meta-data
  (let [md (meta #'simple-view)]
    (is (= (md :name) 'simple-view) "standard meta-data")
    (is (= (md :custom) :bit-of-meta-data) "added meta-data")))

(defview attributes-view
  [req]
  [:p {:id "outer"}
   [:em {:id (req :inner)} (req :message) ]
   linebreak
   [:hr]
   (req :copyright)
   ])

(deftest attribute-rendering
  (serialize-test attributes-view "attribute-rendering" {:message "Nested Text"
                                                         :copyright "(c) 2009 HLS"
                                                         :inner "frotz"}))

(defview special-attribute-values-view []
  [ :p {:class :foo :height 0 :skipped nil} "some text"])

(deftest special-attribute-values
  (serialize-test special-attribute-values-view "special-attribute-values"))

(defn fetch-accounts []
  [{:name "Dewey" :id 595}
   {:name "Cheatum" :id 1234}
   {:name "Howe" :id 4328}])

(defview list-accounts
  []
  [:html
   [:head>title "List Accounts"]
   linebreak
   [:body
    [:h1 "List of Accounts"]
    [:ul
     (for [acct (fetch-accounts)]
       (markup [:li (acct :name)] linebreak))
     ]
    ]
   ])

(deftest inline-macro
  (serialize-test list-accounts "inline-macro"))

(defn looper
  "Loop fragment function. Iterates over its source and updates the req with the value key before
  rendering its body (a block)."
  [req source value-key body]
  (for [value source]
    (body (assoc req value-key value))))

(defview list-accounts-with-loop [req]
  [:html
   [:head>title "List Accounts"]
   linebreak
   [:body
    [:h1 "List of Accounts using (block)"]
    [:ul
                                        ; We'll have to see to what degree block is actually useful;
                                        ; certainly the smple (for) version is easier. I think, ultimately,
                                        ; block will be more about layout components than typical
                                        ; dynamic rendering.
     (looper req (fetch-accounts) :acct (block [req]
                                               [:li (-> req :acct :name)] linebreak))
     ]
    ]
   ])

(deftest block-macro
  (serialize-test list-accounts-with-loop "block-macro" {}))

(defview symbol-view []
  (let [copyright (markup
                   linebreak
                   [:hr]
                   [ :p
                    :&copy " 2009 "
                    [:a {:href "mailto:hlship@gmail.com"} "Howard M. Lewis Ship"]
                    ] linebreak)]
    (markup
     [:html
      [:head>title "Symbol Demo"]
      [:body
       copyright
       [:h1 "Symbol Demo"]
       copyright
       ]
      ])))

(deftest use-of-symbol
  (serialize-test symbol-view "use-of-symbol"))

(defview template-for-view []
  [:ul (markup-for [x [1 2 3]] [:li x])])

(deftest test-template-for
  (serialize-test template-for-view "template-for"))

(defview entity-template []
  [:br]
  :&nbsp
  [:p "After a space"])

(deftest test-entity-in-template
  (serialize-test entity-template "entity-template"))

(defview implicit-attributes []
  [:div
   linebreak [:p.alpha "alpha"]
   linebreak [:p.alpha.bravo "alpha bravo"]
   linebreak [:p.alpha.bravo#tango "tango"]
                                        ; By convention, the #id comes at the end, but the code allows it to be repeated and occur in the middle.
   linebreak [:p.moe#larry.curly#shemp "shemp"]
   linebreak])

(deftest test-implicit-attributes
  (serialize-test implicit-attributes "implicit-attributes"))

(defview comments []
  [:div
   (markup-for [i (range 1 4)]
               (<!-- (format " %d " i)))
   ])

(deftest template-comments (serialize-test comments "template-comments"))

(defview zen-elements-view []
  [:div.alert>p.strong {:foo :bar} "Help!" ])

(deftest zen-elements (serialize-test zen-elements-view "zen-elements-view"))
