# Bultitude

[![Build Status](https://secure.travis-ci.org/Raynes/bultitude.png)](http://travis-ci.org/Raynes/bultitude)

This library is a library similar to `clojure.tools.namespace`. It is
designed to find namespaces on the classpath. This one was ripped from
Leiningen's core because we decided it should be publically available to
everyone.

This library was originally devised in Leiningen because Leiningen had a
few specific needs that `clojure.tools.namespace` did not provide.
Furthermore, the library's author was, at the time, ignoring some issues with
the library (having declined a filed issue about it so far) that makes
the library explode when ran across a namespace with a namespace form
that the reader cannot read. We ran into this problem because
lein-newnew has mustache templates with `.clj` extensions and namespace
forms with mustache syntax inside of them, and it would break any
project that was using tools.namespace. If you have this kind of
problem, you can use this library instead.

Furthermore, this library has a few useful features like being able to
provide your own classpath as a string and for only looking for
namespaces matching a certain prefix.

Note that regarding the above, the author of tools.namespace did eventually fix
the issue. This library is still necessary and maintained because:

* Leiningen uses it internally
* There have been some fairly complex classpath contributions that people rely on
* This is not a contrib project so you can contribute without a CA or a Jira patch parade
* I don't have a lot of confidence in tools.namespace after the issue reported above was declined to be fixed for several months for no good reason

New features have been added to tools.namespace recently, and when I get some time I'll see
about porting them over.

## Usage

```clojure
user=> (require '[bultitude.core :as b])
nil
user=> (take 10 (b/namespaces-on-classpath))
(bultitude.core-test bultitude.core clojure.data clojure.string clojure.test clojure.xml clojure.inspector clojure.repl clojure.set clojure.test.junit)
user=> (b/namespaces-on-classpath :prefix "bultitude")
(bultitude.core-test bultitude.core)
user=> (b/namespaces-on-classpath :prefix "bultitude" :classpath "src")
(bultitude.core)
user=> (b/namespaces-on-classpath :prefix "bultitude" :classpath "src:test")
(bultitude.core bultitude.core-test)
```

Note that the path separator needs to be whatever is used on your
operating system. It can also be a collection of `File` objects. 

## The Name

I don't know. You'd have to ask [Phil](https://github.com/technomancy)
about that one.
