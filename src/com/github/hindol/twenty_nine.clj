(ns com.github.hindol.twenty-nine
  (:require
   [clojure.edn :as edn]
   [com.github.hindol.twenty-nine.events :as events]
   [com.github.hindol.twenty-nine.ws :as ws]
   [io.pedestal.http :as http]
   [io.pedestal.http.jetty.websockets :as websockets]
   [io.pedestal.http.route :as route]))

(def ws-paths
  {"/ws" {:on-connect (fn
                        [ws-session]
                        ((websockets/start-ws-connection ws/add-client) ws-session)
                        (events/dispatch [:join-game {:ws-session ws-session}]))
          :on-text    (fn on-text
                        [ws-session message]
                        (events/dispatch ^{:ws-session ws-session} (edn/read-string message)))
          :on-binary  (fn on-binary
                        [_ws-session payload _offset _length]
                        (println "A client sent - " payload))
          :on-error   (fn on-error
                        [_ws-session e]
                        (println "Socket error - " e))
          :on-close   (fn on-close
                        [ws-session _code _reason]
                        (println "Connection closed.")
                        (swap! ws/clients dissoc ws-session))}})

(def routes
  (route/expand-routes
   #{}))

(def service
  {:env                    :prod
   ::http/routes            routes
   ::http/resource-path     "/public"
   ::http/type              :jetty
   ::http/container-options {:context-configurator #(websockets/add-ws-endpoints % ws-paths {:listener-fn ws/make-listener})}
   ::http/secure-headers    {:content-security-policy-settings "object-src 'none'; script-src 'self' 'unsafe-inline' 'unsafe-eval' https: http: wss: ws:;"}})

(defn -main
  [& {:as args}]
  (-> service
      (merge {::http/host (get args "host")
              ::http/port (Integer/parseInt (get args "port"))})
      http/create-server
      http/start))
