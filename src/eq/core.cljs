(ns eq.core
  (:require [clojure.string :as cs]
            [cljs.reader :as edn]
            [cljs.pprint :as pprint]
            [cljs.nodejs :as node]
            [eq.io :refer [line-seq]]))

(node/enable-util-print!) ; allows (println ..) to print to console.log

(def fs (js/require "fs"))

(defn build-selector
  "Build an eq-like selector"
  [expr]
  (cond
    ;; "", "." -> identity
    (or (nil? expr) (empty? expr) (= expr ".")) identity

    ;; ":foo" -> :foo
    ;; ":foo.bar" -> :foo.bar
    (= (.indexOf expr ":") 0)
      (keyword (.substring expr 1))

    ;; ".foo" = alias for ":foo"
    (= (.lastIndexOf expr ".") 0)
      (keyword (.substring expr 1))

    :else
      ;identity
      nil
    ))

(defn select
  [selector xs]
  (map selector xs))

(defn -main
  [expr & _]
  (if-let [selector (build-selector expr)]
    (doseq [line (->> (. fs openSync "/dev/stdin" "rs")
                      line-seq
                      (map edn/read-string)
                      (select selector))]
      ;; hacky way to avoid double-newlines
      (println
        (cs/replace
          (with-out-str
            (pprint/pprint line))
          #"\n$" "")))

    (do
      (println "Unrecognized argument: " expr)
      (.exit node/process 1))))

(set! *main-cli-fn* -main) ; sends node's process.argv to -main
