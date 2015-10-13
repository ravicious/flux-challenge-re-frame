(ns ^:figwheel-always flux-challenge-re-frame.core
  (:require
    [flux-challenge-re-frame.handlers]
    [reagent.core :as reagent]
    [re-frame.core :as re-frame :refer [subscribe register-sub
                                        dispatch register-handler]])
  (:require-macros
    [reagent.ratom :refer [reaction]]))

(enable-console-print!)

; I don't use prismatic/schema, because I don't want to make the code harder to read for
; someone who doesn't know much ClojureScript.
; On the other hand, using records seems to be useless in this case, see:
; https://twitter.com/Ravicious/status/653604805886275584

; However, I still want to have a place which tells me which fields a given map has . ;)
; (defrecord Planet [id name])
; (defrecord Jedi [id name homeworld apprentice master])

(defonce initial-state {:planet {:id nil :name "unknown"}
                        :jedis []
                        :first-jedi-id 3616
                        :db-initialized? true})

(defn pad
  "Pads the collection coll to the given length n with val"
  [n val coll]
  (take n (concat coll (repeat val))))

; At the start of the application, the re-frame's db is empty. We need to populate it by dispatching
; the initialize-db event, effectively putting contents of the initial-state map into re-frame's db.
(register-handler
  :initialize-db
  (fn [db _]
    (if (:db-initialized? db)
      db
      initial-state)))

;;; subs

(defn jedis [db, _]
  (reaction (:jedis @db)))

(register-sub :jedis jedis)

(defn padded-jedis [db, [_, count]]
  (->> @(jedis db _)
       (pad count nil)
       reaction))

(register-sub :padded-jedis padded-jedis)

(defn current-planet [db, _]
  (reaction (:planet @db)))

(register-sub :current-planet current-planet)


;;; initial dispatches

(dispatch [:initialize-db])
(dispatch [:monitor-planets])
(dispatch [:populate-jedis])

;;; components

(defn planet-monitor [planet]
  [:h1 {:class "css-planet-monitor"}
   (str "Obi-Wan currently on " (:name planet))])

(defn jedi-slot [jedi]
  [:li {:class "css-slot"}
   (when jedi
     [:span
      [:h3 (:name jedi)]
      [:h6 (str "Homeworld: " (get-in jedi [:homeworld :name]))]])])

(defn dark-jedi-list []
  (let [current-planet (subscribe [:current-planet])
        padded-jedis (subscribe [:padded-jedis 5])]
    (fn []
      [:div {:class "css-root"}
       [planet-monitor @current-planet]

       [:section {:class "css-scrollable-list"}
        [:ul {:class "css-slots"}
         (for [jedi @padded-jedis]
           (let [key (or
                       (:name jedi)
                       (gensym "jedi"))] ; use random name if no name present
             ^{:key key} [jedi-slot jedi]))]

        [:div {:class "css-scroll-buttons"}
         [:button {:class "css-button-up"}]
         [:button {:class "css-button-down"}]]]])))

(reagent/render-component
  [dark-jedi-list]
  (. js/document (getElementById "app")))

(defn on-js-reload []
  ;; optionally touch your app-state to force rerendering depending on
  ;; your application
  ;; (swap! app-state update-in [:__figwheel_counter] inc)
  )
