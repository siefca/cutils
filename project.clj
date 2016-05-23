(defproject pl.randomseed/cutils "1.0.0"
  :description "Collection processing utils for Clojure"
  :url         "https://randomseed.pl/software/cutils"

  :license {:name "LGPL License", :url "https://opensource.org/licenses/lgpl-3.0.html"}
  :scm     {:name "git", :url "https://github.com/siefca/cutils"}

  :profiles {:dev {:dependencies [[midje                   "1.8.3"]
                                  [helpshift/hydrox       "0.1.15"]
                                  [im.chit/vinyasa.inject  "0.3.4"]]
                   :plugins      [[lein-midje                "3.2"]
                                  [lein-environ            "1.0.1"]]
                   :injections   [(require '[vinyasa.inject :as inject])
                                  (inject/in [hydrox.core dive surface generate-docs
                                              import-docstring purge-docstring])]
                   :env          {:dev-mode true}}}

  :dependencies [[org.clojure/clojure "1.8.0"]]

  :documentation {:site "cutils"
                  :description "Collection processing utils for Clojure"
                  :owners [{:name    "Paweł Wilk"
                            :email   "pw@gnu.org"
                            :website "https://randomseed.pl/"}]
                  :output "docs"
                  :paths ["src-doc"]
                  :template {:path "template"
                             :copy ["assets"]
                             :defaults {:template     "article.html"
                                        :navbar       [:file "partials/navbar.html"]
                                        :dependencies [:file "partials/deps-web.html"]
                                        :navigation   :navigation
                                        :article      :article}}
                  :files {"index"
                          {:input     "src-doc/cutils/overview.clj"
                           :title     "Cutils"
                           :subtitle  "Collection processing utils for Clojure"}}
                  :link {:auto-tag    true
                         :auto-number true}})
