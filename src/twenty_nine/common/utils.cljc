(ns twenty-nine.common.utils)

(defn positions
  [pred coll]
  (keep-indexed (fn [idx x]
                  (when (pred x)
                    idx))
                coll))

(defn position
  [pred coll]
  (first (positions pred coll)))
