(ns vdd.player
  (:require-macros [hiccups.core :as hiccups])
  (:require [hiccups.runtime :as hiccupsrt]
            [jayq.core :as jq])
  (:use [vdd.util :only [log]]))

; TODO This jams too much all in one file. We should try to abstract away some of it like the 
; button stuff and the slider.

(def player-control 
  (hiccups/html 
    [:div.player
     [:div.btn-toolbar 
      [:div.btn-group
       [:a.btn.first {:href "#"}[:i.icon-step-backward]]
       [:a.btn.back {:href "#"}[:i.icon-backward]]
       [:a.btn.play {:href "#"}[:i.icon-play]]
       [:a.btn.pause {:href "#"}[:i.icon-pause]]
       [:a.btn.forward {:href "#"}[:i.icon-forward]]
       [:a.btn.last {:href "#"}[:i.icon-step-forward]]]]
     [:div.slider]]))

(defn- button-state-helper 
  "A helper function that will unwrap the state of the player state atom and invoke the 
  given button function."
  [player-state-atom button-fn]
  (let [player-state @player-state-atom
        items (:items player-state)
        data-handler (:data-handler player-state)
        item-index (:index player-state)
        new-state (button-fn player-state items data-handler item-index)]
    (when new-state (reset! player-state-atom new-state))))


(defn- play 
  "Handles the play button press."
  [player-state items data-handler item-index]
  )

(defn- pause 
  "Handles the pause button press."
  [player-state items data-handler item-index]
  )

(defn- go-to-index
  "Helper function to go to a specific index"
  [player-state items data-handler new-index]
  (if (and (>= new-index 0)
           (< new-index (count items))
           (not= new-index (:index player-state)))
    (let [item (nth items new-index)]
      (data-handler item)
      ; Update the slider value
      (.slider (:slider player-state) "value" new-index)
      
      (assoc player-state :index new-index))
    ; else don't change state
    player-state))

(defn- back 
  "Handles the back button press."
  [player-state items data-handler item-index]
  (go-to-index player-state items data-handler (dec item-index)))

(defn- forward 
  "Handles the forward step button press."
  [player-state items data-handler item-index]
  (go-to-index player-state items data-handler (inc item-index)))

(defn- jump-to-first
  "Handles the jump to first button press"
  [player-state items data-handler item-index]
  (go-to-index player-state items data-handler 0))

(defn- jump-to-last 
  "Handles the jump to last button press"
  [player-state items data-handler item-index]
  (go-to-index player-state items data-handler (-> items count dec)))
    
(defn- create-player-state 
  "Creates the initial state of the player control"
  ([slider] 
   (create-player-state slider [] (fn [_] nil)))
  ([slider items data-handler]
   {:items items
    :data-handler data-handler
    :index -1
    :playing true
    :slider slider}))

(defn- update-slider-state
  [slider items]
  ())

(defn- player-data-handler 
  "Callback function that will be returned when a player control is constructed. It will
  take the items to play and a data handler to accept each item"
  [player-state-atom items data-handler]
  (let [player-state @player-state-atom
        slider (:slider player-state)
        player-state (create-player-state slider items data-handler)
        player-state (go-to-index player-state items data-handler 0)]
    ; Set the max state of the slider
    (.slider (:slider player-state) "option" "max" (dec (count items)))
    
    (reset! player-state-atom player-state)
    (log "Player data set!")))

(defn- setup-button 
  "Adds a click handler to a button within the player
  Params:
    player - the player element on the page
    state-atom - an atom that references the current player state
    type - the button type. ie :pause
    handler-fn - the function that should be called when the button is clicked."
  [player state-atom type handler-fn]
  (let [btn (.find player (str "a." (name type)))
        btn-click-handler (partial button-state-helper state-atom handler-fn)]
    (.click btn btn-click-handler)))

(defn ^:export createPlayerFn
  "Creates a player component within the given element and returns a function
  that can be used to set the event data and handler function. Click handlers are assigned
  to all of the buttons in the player component created. The functions have been 
  partially applied to a shared atom that is used to set the state."
  [element]
  (log (str "Creating a player within " element))
  (let [player (jq/append element player-control)
        slider-options {:min 0 :value 0 :step 1 :max 0}
        slider (.slider (.find player "div.slider") slider-options)
        player-state-atom (atom (create-player-state slider))
        button-types-and-fns {:play play :pause pause :back back :forward forward
                              :first jump-to-first :last jump-to-last}]
    ; Add click handlers to the buttons
    (doseq [[btn-type btn-fn] button-types-and-fns]
      (setup-button player player-state-atom btn-type btn-fn))
    
    ; Return a function that will allow setting the data and data-handler function
    (partial player-data-handler player-state-atom)))

