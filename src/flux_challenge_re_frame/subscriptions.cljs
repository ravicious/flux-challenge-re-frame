(ns flux-challenge-re-frame.subscriptions
  (:require
    [flux-challenge-re-frame.utils :as utils]
    [re-frame.core :refer [register-sub]]
    )
  (:require-macros
    [reagent.ratom :refer [reaction]]
    )
  )

(defn- jedis [db, _]
  (reaction (:jedis @db)))

(register-sub :jedis jedis)

(defn- padded-jedis [db, [_, count]]
  (->> @(jedis db _)
       (utils/pad count nil)
       reaction))

(register-sub :padded-jedis padded-jedis)

(defn- current-planet [db, _]
  (reaction (:planet @db)))

(register-sub :current-planet current-planet)
