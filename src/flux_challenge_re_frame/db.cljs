(ns flux-challenge-re-frame.db
  (:require
    [re-frame.core :as re-frame :refer [register-handler]]
    )
  )

; I don't use prismatic/schema, because I don't want to make the code harder to read for
; someone who doesn't know much ClojureScript.
; On the other hand, using records seems to be useless in this case, see:
; https://twitter.com/Ravicious/status/653604805886275584

; However, I still want to have a place which tells me which fields a given map has. ;)
; (defrecord Planet [id name])
; (defrecord Jedi [id name homeworld apprentice master])

(defonce
  ^{:private true}
  initial-state {:planet {:id nil :name "unknown"}
                 :jedis []
                 :first-jedi-id 3616
                 :db-initialized? true})

; At the start of the application, the re-frame's db is empty. We need to populate it by dispatching
; the initialize-db event, effectively putting contents of the initial-state map into re-frame's db.
(register-handler
  :initialize-db
  (fn [db _]
    (if (:db-initialized? db)
      db
      initial-state)))
