(ns twenty-nine.backend.api
  (:require
   [clojure.pprint :as pp]
   [io.pedestal.http.ring-middlewares :as ring]
   [io.pedestal.http.route :as route]
   [twenty-nine.common.utils :as u])
  (:import
   (java.util UUID)))

(defn uuid
  []
  (UUID/randomUUID))

(defn authenticate
  [access-tokens request]
  (let [token (str (uuid))]
    (swap! access-tokens assoc token (:remote-addr request))
    {:status  200
     :body    token
     :cookies {:access-token {:value token
                              :path  "/"}}}))

(defn authorized?
  [access-tokens token {:keys [remote-addr]}]
  (let [[_ addr] (find @access-tokens token)]
    (= addr remote-addr)))

(defn authorize
  [access-tokens]
  {:enter (fn [context]
            (u/when-let* [token       (-> context :body-params :access-token)
                          remote-addr (-> context :request :remote-addr)]
              (assoc context :authorized?
                     (authorized? access-tokens token {:remote-addr remote-addr}))))})

(defn create-routes
  [{:keys [access-tokens]}]
  (route/expand-routes
   #{["/token/access" :get [ring/cookies (partial authenticate access-tokens)] :route-name :authenticate]}))
