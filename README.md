# tta

A [re-frame](https://github.com/Day8/re-frame) application designed to ... well, that part is up to you.

## Development Mode

### Run application:

```
lein clean
lein repl
(fig-start)
```

Figwheel will automatically push cljs changes to the browser.

Wait a bit, then browse to [http://localhost:3450](http://localhost:3450).

From you IDE, connect to the repl and in that type following to start the cljs repl
```
(cljs-repl)
```

To build the static file style.css
```
lein garden once
```

## Production Build


To compile clojurescript to javascript:

```
lein clean
lein build
```
