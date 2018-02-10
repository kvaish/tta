(ns tta.app.d3
  (:require [reagent.core :as r]
            [reagent.dom :as dom]
            [cljsjs.d3]
            [ht.util.interop :as i]))

"class on each node is required for selection purpose.
  it should be unique enough to have clear selection.
  the schema of node:
  {:tag <:g|:rect|..>
   :class <class name as keyword>
   :style {:key <value | keyword | [key seq ..] | (fn [data i idv])>}
   :attrs {:key <value | keyword | [key seq ..] | (fn [data i idv])>}
   :text <value | keyword | [key seq ..] | (fn [data i idv])>
   :on-off-classes <{:on \"on classes ..\", :off \"off classes ..\"} | (fn [data i idv])>
   :data <keyword | [key seq..] (fn [data i idv])>
   :skip? (fn [data i idv])
   :multi? <true | false>
   :nodes [child nodes..]
   :did-append (fn [sel-node i idv])
   :did-update (fn [sel-node data i idv]) }"

(defn- cache-update [state idv data]
  ;; (js/console.log "cache-update" idv)
  (swap! state update :cache update-in idv #(with-meta (or % {}) data)))

(defn- cache-clear [state idv]
  ;; (js/console.log "cache-clear" idv)
  (swap! state update :cache assoc-in idv {}))

(defn- cache-get [state idv]
  (meta (get-in (:cache @state) idv)))

(defn- append-node [sel-parent node index idv]
  (assert (and (keyword? (:tag node))
               (keyword? (:class node)))
          (str "tag and class must be keyword in node at " idv))

  (let [{:keys [tag class multi? skip?]} node
        tag (name tag)
        class (name class)
        coll-class (if (or multi? skip?) (str class "-coll"))
        sel-node ;; create this node
        (-> sel-parent
            (i/ocall :append (if coll-class "g" tag))
            (i/ocall :attr "class" (or coll-class class)))]
    ;; create child nodes if self is not a coll node
    (if-not (or multi? skip?)
      (doseq [node (:nodes node)]
        (append-node sel-node node index (conj idv (:class node)))))
    ;; custom append function
    (if-let [afn (:did-append node)]
      (afn sel-node index idv))
    ;; return this node
    sel-node))

(declare update-node)

(defn get-value [vf data index idv]
  (cond
    (fn? vf) (vf data index idv)
    (keyword? vf) (vf data)
    (vector? vf) (get-in data vf)
    :default vf))

(defn- update-props [sel-node node data index idv]
  ;; (js/console.log "updated" (name (:class node)) data)
  (if (contains? node :text)
    (i/ocall sel-node :text (get-value (:text node) data index idv)))
  (if (contains? node :on-off-classes)
    (let [{:keys [on off]}
          (get-value (:on-off-classes node) data index idv)]
      (if on (i/ocall sel-node :classed on true))
      (if off (i/ocall sel-node :classed off false))))
  (if (contains? node :style)
    (doseq [[k vf] (:style node)]
      (i/ocall sel-node :style (name k) (get-value vf data index idv))))
  (if (contains? node :attrs)
    (doseq [[k vf] (:attrs node)]
      (i/ocall sel-node :attr (name k) (get-value vf data index idv)))))

(defn- update-node-coll [ele-coll node data-coll sel-parent pidv state]
  ;; remove extra
  (doseq [[index ele] (->> ele-coll
                           (map #(list %1 %2) (range))
                           (drop (count data-coll)))]
    (cache-clear state (conj pidv index))
    (-> js/d3
        (i/ocall :select ele)
        (i/ocall :remove)))
  ;; update remaining and newly added
  (doseq [[index data ele] (map list (range) data-coll
                                (concat ele-coll (repeat nil)))]
    ;; (js/console.log [index data ele])
    (let [idv (conj pidv index)]
      (-> (if ele
            ;; get existing
            (i/ocall js/d3 :select ele)
            ;; new - append one
            (append-node sel-parent (dissoc node :multi? :skip?) index idv))
          (update-node node data index idv state)))))

(defn- update-node
  [sel-node node data index idv state]
  #_(js/console.log "test update" (name (:class node))
                    (not (:data node))
                    (not= data (get-in @state [:cache idv]))
                    [data (get-in @state [:cache idv])])
  ;; if no data spec given in node, then it should update when ever the
  ;; parent is updated. otherwise check if its own data has changed.
  (when (or (not (:data node))
            (not= data (cache-get state idv)))
    (cache-update state idv data)
    (update-props sel-node node data index idv)
    ;; child nodes
    (doseq [node (:nodes node)]
      ;; (js/console.log "updating" (name (:class node)))
      (let [{:keys [tag class skip? multi?]} node
            cidv (conj idv class)
            tag (name tag)
            class (name class)
            coll-class (if (or multi? skip?) (str class "-coll"))
            child-data (as-> (or (get-value (:data node) data index idv) data) $
                         ;; the skip? is handled by treating as a coll with only
                         ;; entry or no entry. if both skip? and multi? is
                         ;; specified, then be careful not to add double []
                         (if-not skip?
                           $ ;; not a skippable node
                           (if (get-value (:skip? node) $ index idv) []
                               (if (get-value (:multi? node) $ index idv) $ [$]))))]
        ;; (js/console.log "updating with data" class data)
        (if-not coll-class
          (-> (i/ocall sel-node :select (str tag "." class))
              (update-node node child-data index cidv state))
          (let [sel-parent (i/ocall sel-node :select (str "g." coll-class))]
            (-> sel-parent
                (i/ocall :selectAll (str (name tag) "." (name class)))
                (i/ocall :nodes)
                (update-node-coll node child-data sel-parent cidv state))))))
    ;; custom update function if provided
    (if-let [ufn (:did-update node)]
      (ufn sel-node data index idv)))
  ;; return this node
  sel-node)

(defn d3-svg
  "**props**: map with width, height, view-box, preserve-aspect-ratio
  style, class, node, data"
  [props]
  (let [state (atom {})]
    (r/create-class
     {:reagent-render
      (fn [{:keys [width height view-box preserve-aspect-ratio style class]
           :or {width "100%", height "100%"
                preserve-aspect-ratio "xMidYMid meet"}}]
        [:svg {:view-box view-box
               :preserve-aspect-ratio preserve-aspect-ratio
               :style style, :class class
               :width width, :height height}])

      :component-did-mount
      (fn [this]
        (let [ele (dom/dom-node this)
              {:keys [node data]} (r/props this)]
          (let [idv [(:class node)]
                root (-> js/d3
                         (i/ocall :select ele)
                         (append-node node nil idv)
                         (update-node node data nil idv state))]
            (swap! state assoc :ele ele, :root root, :node node))))

      :component-did-update
      (fn [this _]
        ;; (js/console.log "root did update")
        (let [{:keys [root node]} @state
              {:keys [data]} (r/props this)]
          (swap! state assoc :data data)
          (update-node root node data nil [(:class node)] state)))})))

(defn d3-svg-2-string
  "**props**: map with width, height, view-box, preserveAspectRatio,
  style, class, node, data"
  [props]
  (let [{:keys [width height view-box preserve-aspect-ratio
                style class node data]
         :or {preserve-aspect-ratio "xMidYmid meet"}} props
        idv [(:class node)]
        state (atom {})
        div (-> js/d3
                (i/ocall :select "body")
                (i/ocall :append "div")
                (i/ocall :attr "class" "svg-export-container")
                (i/ocall :attr "style" "display:none;"))
        svg (-> div
                (i/ocall :append "svg")
                (i/ocall :attr "width" width)
                (i/ocall :attr "height" height)
                (i/ocall :attr "viewBox" view-box)
                (i/ocall :attr "preserveAspectRatio" preserve-aspect-ratio)
                (i/ocall :attr "class" class))]
    (doseq [[k v] style]
      (i/ocall svg :style (name k) v))
    (append-node svg node nil idv)
    (update-node svg node data nil idv state)

    ;; svg node ready.. now export
    (let [svg-string (js/getSVGString (i/ocall svg :node))]
      (i/ocall div :remove) ;; clean up
      ;; return the svg string
      svg-string)))
