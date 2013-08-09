(ns vdd.util)

(defn log [str]
  (.log js/console str))

(defn js-obj->map 
  "Converts a javascript object to a map with keyword keys"
  [jo]
  (into {} (for [[k v] (js->clj jo)]
             [(keyword k) v])))