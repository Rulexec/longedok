Longedok
========
Just music, no bullshit!
------------------------

# API
## HTTP
* ```<date>``` — 64-bit uint server unix time in hex
* ```<bool>``` — "true" for true, "false" for false

All responses are JSON objects, that has following fields:

```
{date: <date>}
```

If status code is 200, then request is successful and response will be in "response" field.

```
{response: ..., ...}
```

or, if error was happened:

```
{error: "string_id"[, reason: "some optional description"], ...}
```

### /simple/0.0.1
#### /tracks
* ```<track_id>``` — 64-bit uint in hex

##### GET /simple/0.0.1/tracks/all
Arguments:

* ```from=<track_id/0>```
* ```limit=<pnat>``` — how many items to return. In diff mode number of deletions can be more than limit. Implementation should limit number of deletions to achieve correspondence of their total size in bytes to average total size of limit' tracks data.
* ```exclusive=<bool/false>``` — exclude or not track with id = from
* ```lastModified=<date> [optional]``` — if present, returns tracks, that has been modified after specified date.

```
{
  tracks: [{
    id: <track_id>,
    lastModified: <date>,

    ... TODO ...
  }],
  isEnd: <bool>,
  [deleted: [{id: <track_id>}, ...]]
}
```

##### GET /simple/0.0.1/tracks/&lt;track_id&gt;.mp3

##### POST /simple/0.0.1/tracks/search/simple?q=&lt;utf8&gt;&amp;limit=&lt;nat&gt;
Initiates search. Returns first part of results. Next parts must be accessed by /search/continue calls with corresponding &lt;searchId&gt;.

```
{
  tracks: [...],
  isEnd: <bool>

  searchId: <searchId>,
  expiresAt: <date>,
  approximateResults: <pnat>
}
```

##### POST /simple/0.0.1/tracks/search/continue/&lt;searchId&gt;
Response is the same as at start of a search. But it also can be:

```
{expired: true}
```

##### POST /simple/0.0.1/tracks/search/renew/&lt;searchId&gt;
If *searchId* is about to expire, but user maybe still want to continue viewing results later, token can be renewed.

```
{expiresAt: <date>}
```
