(ns twenty-nine.backend.service
  (:require
   [io.pedestal.http :as http]
   [io.pedestal.http.route :as route]
   [io.pedestal.http.jetty.websockets :as ws]
   [io.pedestal.http.ring-middlewares :as ring]
   [twenty-nine.backend.ws]
   [twenty-nine.common.utils :as u]))

(def ^:private routes
  (route/expand-routes #{}))

(def ^:private ws-paths
  {"/ws" twenty-nine.backend.ws/fn-map})

(defmulti create-service-map :env)

(defmethod create-service-map :prod
  [_]
  {:env                    :prod
   ::http/routes            routes
   ::http/resource-path     "/public"
   ::http/type              :jetty
   ::http/container-options {:context-configurator #(ws/add-ws-endpoints % ws-paths {:listener-fn twenty-nine.backend.ws/connection-listener})}
   ::http/secure-headers    {:content-security-policy-settings "object-src 'none'; script-src 'self' 'unsafe-inline' 'unsafe-eval' https: wss:;"}})

(defmethod create-service-map :dev
  [_]
  (-> (merge (create-service-map {:env :prod})
             {:env                  :dev
              ::http/routes          #(deref #'routes)
              ::http/join?           false
              ::http/resource-path   "/dev"
              ::http/mime-types      {nil "text/html"} ;; No extension in URL => assume HTML
              ::http/allowed-origins {:creds           true
                                      :allowed-origins (constantly true)}
              ::http/secure-headers  {:content-security-policy-settings "object-src 'none'; script-src 'self' 'unsafe-inline' 'unsafe-eval' http: ws:;"}})
      http/default-interceptors
      (update ::http/interceptors (fn prefer-fast-resource [interceptors]
                                    (if-let [idx (u/position #(-> % :name (= ::ring/resource)) interceptors)]
                                      (assoc interceptors idx (ring/fast-resource "/dev"))
                                      interceptors)))
      http/dev-interceptors))
