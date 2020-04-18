(ns twenty-nine.backend.system
  (:require
   [com.stuartsierra.component :as c]
   [twenty-nine.backend.server :as server]))

(defn create-system
  [{:keys [service-map]}]
  (c/system-map
   :web-server (server/create-web-server service-map)))
