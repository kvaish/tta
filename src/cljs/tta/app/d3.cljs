(ns tta.app.d3
  (:require [reagent.core :as r]
            [reagent.dom :as dom]
            [ht.util.interop :as i]))

"class on each node is required for selection purpose.
  it should be unique enough to have clear selection.
  the schema of node:
  {:tag <:g|:rect|..>
   :class <class name as keyword>
   :attrs {:key <value | (fn [data])>}
   :data-path [key seq..]
   :data-fn (fn [data])
   :skip? (fn [data])
   :multi? <true | false>
   :nodes [child nodes..]
   :did-append (fn [sel-node idv])
   :did-update (fn [sel-node data idv]) }"

(defn- append-node [sel-parent node idv]
  (let [{:keys [tag class multi? skip?]} node
        tag (name tag)
        class (name class)
        coll-class (if (or multi? (fn? skip?)) (str class "-coll"))
        sel-node ;; create this node
        (-> sel-parent
            (i/ocall :append (if coll-class "g" tag))
            (i/ocall :attr "class" (or coll-class class)))]
    ;; create child nodes
    (doseq [node (:nodes node)]
      (append-node sel-node node (conj idv (:class node))))
    ;; custom append function
    (if-let [afn (:did-append node)]
      (afn sel-node idv))
    ;; return this node
    sel-node))

(declare update-node)

(defn- apply-attrs [sel-node attrs data idv]
  (doseq [[k v-or-fn] attrs]
    (i/ocall sel-node :attr (name k)
             (if (fn? v-or-fn)
               (if idv (v-or-fn data idv)
                   (v-or-fn data))
               v-or-fn))))

(defn- update-node-coll [ele-coll node data-coll sel-parent pidv]
  ;; remove extra
  (doseq [ele (drop (count data-coll) ele-coll)]
    (-> js/d3
        (i/ocall :select ele)
        (i/ocall :remove)))
  ;; update remaining and newly added
  (doseq [[index data ele] (map list (range) data-coll
                                (concat ele-coll (repeat nil)))]
    (let [idv (conj pidv index)]
      (-> (if ele (i/ocall js/d3 :select ele)
              ;; add new
              (append-node sel-parent node idv))
          (update-node node data idv)))))

(defn- update-node [sel-node node data idv]
  (when (not= data (i/ocall sel-node :datum))
    (-> sel-node
        (i/ocall :datum data)
        (apply-attrs (:attrs node) data idv))
    ;; child nodes
    (doseq [node (:nodes node)]
      (let [{:keys [tag class data-path data-fn skip? multi?]} node
            cidv (conj idv class)
            tag (name tag)
            class (name class)
            coll-class (if (or multi? (fn? skip?)) (str class "-coll"))
            child-data (as-> (cond
                               data-fn (data-fn data)
                               data-path (get-in data data-path)
                               :default data) $
                         ;; the skip? is hancled by treating as a coll with only
                         ;; entry or no entry. if both skip? and multi? is
                         ;; specified, then be careful not to add double []
                         (if (fn? skip?)
                           (if (skip? data) []
                               (if multi? $ [$]))
                           $))]
        (if-not coll-class
          (-> (i/ocall sel-node :select (str tag "." class))
              (update-node node child-data cidv))
          (let [sel-parent (i/ocall sel-node :select (str "g." coll-class))]
            (-> sel-parent
                (i/ocall :selectAll (str (name tag) "." (name class)))
                (i/ocall :nodes)
                (update-node-coll node child-data sel-parent cidv))))))
    ;; custom update function if provided
    (if-let [ufn (:did-update node)]
      (ufn sel-node data idv)))
  ;; return this node
  sel-node)


(defn d3
  "**props**: map with width, height, view-box, style, class
  and node, data"
  [props]
  (let [state (atom {})]
    (r/create-class
     {:reagent-render
      (fn [{:keys [width height view-box style class]
           :or {:width "100%", :height "100%"}}]
        [:svg {:view-box view-box
               :style style, :class class
               :width width, :height height}])

      :component-did-mount
      (fn [this]
        (let [ele (dom/dom-node this)
              {:keys [node data]} (r/props this)]
          (let [idv [(:class node)]
                root (-> js/d3
                         (i/ocall :select ele)
                         (append-node node idv)
                         (update-node node data idv))]
            (swap! state assoc :ele ele, :root root
                   :node node, :data data))))

      :component-did-update
      (fn [this [_ props]]
        (let [{:keys [root node data]} @state
              {new-data :data} props]
          (when (not= data new-data)
            (update-node root node new-data [(:class node)])
            (swap! state assoc :data new-data))))})))
