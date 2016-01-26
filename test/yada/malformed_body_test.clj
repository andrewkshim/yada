(ns yada.malformed-body-test
  (:require [clojure.test :refer :all]
            [yada.resource :refer [resource]]
            [yada.handler :refer [handler]]
            [schema.core :as sc]
            [ring.mock.request :as mock]
            [clojure.edn :as edn]
            [byte-streams :as bs]))

(defn to-string [s]
  (bs/convert s String))

(deftest schema-error-is-available-in-context-error
  (let [resource     (resource {:consumes  [{:media-type #{"application/edn"
                                                           "application/x-www-form-urlencoded"}}]
                                :produces  [{:media-type #{"application/edn"}}]
                                :methods   {:post {:parameters {:body {:foo sc/Int}}
                                                   :response   identity}}
                                :responses {400 {:produces [{:media-type #{"application/edn"}}]
                                                 :response (fn [ctx]
                                                             (-> ctx :error ex-data :error))}}})
        handler      (handler resource)
        edn-request  (-> (mock/request :post "/" (pr-str {:foo :asdf}))
                         (mock/content-type "application/edn"))
        form-request (mock/request :post "/" {:foo :asdf})]

    (let [response @(handler edn-request)]
      (is (some? response))
      (is (= 400 (:status response)))
      (let [body (-> response :body to-string edn/read-string)]
        (is (= '{:foo (not (integer? :asdf))} body))))

    (let [response @(handler form-request)]
      (is (some? response))
      (is (= 400 (:status response)))
      (let [body (-> response :body to-string edn/read-string)]
        (is (= '{:foo (not (integer? :asdf))} body))))))
