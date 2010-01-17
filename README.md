CrashKit APIs
=============

[CrashKit](http://crashkitapp.appspot.com/) is a hosted solution for collecting,
tracking and analyzing unhandled exceptions in your applications, Web and desktop ones alike.

Your application talks to CrashKit servers via a JSON API described here.
The necessary code for many popular languages is provided in this repository,
so chances are integration will be very easy for you.


Supported languages and frameworks
----------------------------------

1. Java. Production state.
Supports running under OSGi/Eclipse, but does not integrate with Eclipse error log yet.
Will be refactored soon for simplification and to become a single file.
See [Java tickets](https://yoursway.lighthouseapp.com/projects/27411-crashkit/tickets?q=tagged%3Ajava+state%3Aopen) in our bug tracker.

2. PHP. Production state.
See [PHP tickets](https://yoursway.lighthouseapp.com/projects/27411-crashkit/tickets?q=tagged%3Aphp+state%3Aopen) in our bug tracker.

3. Python. Production state. Supports Django and Google App Engine applications.
See [Python tickets](https://yoursway.lighthouseapp.com/projects/27411-crashkit/tickets?q=tagged%3Apython+state%3Aopen) in our bug tracker.

4. JavaScript. Beta state, has some support for jQuery, other frameworks support is planned.
See [JavaScript tickets](https://yoursway.lighthouseapp.com/projects/27411-crashkit/tickets?q=tagged%3Ajavascript+state%3Aopen) in our bug tracker.

If your language is not supported yet, you can roll your own implementation
using the API documentation. We'd be happy to include the code for your language
in our distribution if you choose to contribute it.

Support for the following languages is planned:

* C# (see [the tickets](https://yoursway.lighthouseapp.com/projects/27411-crashkit/tickets?q=tagged%3Ac%23+state%3Aopen))
* Ruby (see [the tickets](https://yoursway.lighthouseapp.com/projects/27411-crashkit/tickets?q=tagged%3Aruby+state%3Aopen))

The goal is for each language implementation to be a single file with no dependencies.
This way it is easy to drop a file into your project and easy to update it afterwards.


JSON API
--------

The API will become stable in February 2010. At that points, the URLs will change to include
API version numbers. This section documents the current API.

The API is constantly evolving. See [API-related tickets](https://yoursway.lighthouseapp.com/projects/27411-crashkit/tickets?q=tagged%3Aapi+state%3Aopen) in our bug tracker.

All API replies are url-encoded strings
(because url-decoding is readily available in most lanugages,
and easy to do manually even if it is not supported by your one).

**Posting exceptions.**
Exceptions, formatted as a JSON payload, should be POSTed to:

    http://crashkitapp.appspot.com/youraccount/products/yourproduct/post-report/0/0
    
Example answer is:

    response=ok
    
**Instance tracking.**
For desktop applications, it is possible to track which instance (installation) of your application
the exception came from. To do this, before sending the first exception, you need to GET the following URL:

    http://crashkitapp.appspot.com/youraccount/products/yourproduct/obtain-client-id
    
Example answer is:

    response=ok&client_id=117&client_cookie=abs43d4s534lf
    
Store the client ID and cookie somewhere, and provide them inside the URL when posting your exceptions. Example:

    http://crashkitapp.appspot.com/youraccount/products/yourproduct/post-report/117/abs43d4s534lf
    
**Testing.**
For the test account only, there is an API to obtain the last exception received by CrashKit.
(This is used by the test suite.)

    http://crashkitapp.appspot.com/test/products/someproduct/last-posted-report


### JSON payload ###

The following is an example payload:

    [
        {
            "client_version": "1.0.3", 
            "data": {
                "G_boz": "biz", 
                "G_foo": "bar"
            }, 
            "env": {
                "HTTP_ACCEPT_ENCODING": "identity", 
                "HTTP_CONNECTION": "close", 
                "HTTP_HOST": "localhost:8236", 
                "HTTP_USER_AGENT": "Python-urllib/2.5", 
                "PATH_INFO": "/polls/1/vote/", 
                "QUERY_STRING": "foo=bar&boz=biz", 
                "REMOTE_ADDR": "127.0.0.1", 
                "REMOTE_HOST": "", 
                "SERVER_NAME": "www.example.com", 
                "SERVER_PORT": "8236", 
                "SERVER_PROTOCOL": "HTTP/1.1", 
                "SERVER_SOFTWARE": "WSGIServer/0.1 Python/2.5.1", 
                "cpu_arch": "32bit", 
                "cpu_type": "i386", 
                "os_kernel_name": "Darwin", 
                "os_kernel_version": "9.8.0", 
                "python_version": "2.5.1"
            }, 
            "exceptions": [
                {
                    "locations": [
                        {
                            "claimed": true, 
                            "file": "/Users/andreyvit/Projects/feedback/python-client/crashkit_django/polls/views.py", 
                            "line": 21, 
                            "method": "vote", 
                            "package": "crashkit_django.polls.views"
                        }, 
                        {
                            "claimed": false, 
                            "class": "WSGIHandler", 
                            "file": "/Library/Python/2.5/site-packages/django/core/handlers/base.py", 
                            "line": 86, 
                            "method": "get_response", 
                            "package": "django.core.handlers.base"
                        }
                    ], 
                    "message": "integer division or modulo by zero", 
                    "name": "ZeroDivisionError"
                }
            ], 
            "language": "python", 
            "role": "customer"
        }
    ]
    
The payload is an array of exceptions. Each exception is a JSON object with the following keys:

* `data` contains information that is expected to vary with each exception

* `data.G_*`, `data.P_*`, `data.C_*` and `data.S_*` are recognized as GET, POST, cookie and session variables, 
and are rendered accordingly

* `env` contains information which is expected to stay the same for the given instance of your
application running on the given software configuration; many keys here are treated specially

* `exceptions` contains the stack trace of the exception; this is an array which would only contain
a single element for many languages. Some languages (like Java) whose exceptions carry a linked
list of “causes” will want to provide several items here. 

* `exceptions[0].name` is exception kind/class/etc; exceptions with different values in this field
will be grouped into different bugs, so you don't want anything too variable here.
Even if you can only come up with several kinds (like JavaScript which has 3 kinds of exceptions),
you only want those kinds here.

* `exceptions[0].message` is the message — not used for grouping, so can vary with each occurrence

* `exceptions[0].locations[0].file` — absolute file name

* `exceptions[0].locations[0].line` — 1-based line number

* `exceptions[0].locations[0].method` — method/function name, if any

* `exceptions[0].locations[0].class` — class name, if any

* `exceptions[0].locations[0].package` — installation-independent, platform-independent file name

* `exceptions[0].locations[0].claimed` — whether this location is inside your code (true)
or inside independent library code (false); used to group exceptions by the first location
 which is inside your code, as opposite to simply the first location
 
 
New stable 1.0 API
------------------

The 1.0 API is an unreleased work-in-progress. This section documents how it will work after being released.

POST JSON payload to:

    http://crashkitapp.appspot.com/api/1/youraccount/products/yourproduct/reports
    
Limitation of the API: posting the payload should not require actually parsing it. To parse and process the payload immediately, use:

    http://crashkitapp.appspot.com/api/1/youraccount/products/yourproduct/reports?debug=1
    
Any problems with the JSON payload will be reported back to you. This additionally enables a strict processing mode, so that no invalid reports will be accepted.

For testing support, a test token (any unique string, preferable a GUID) can be passed:

    http://crashkitapp.appspot.com/api/1/youraccount/products/yourproduct/post-report?token=abcbc231232
    
The latest reports corresponding to the given token can then be queried via:

    http://crashkitapp.appspot.com/api/1/youraccount/products/yourproduct/latest-report?token=abcbc231232

**Payload**
    
Each piece of data you provide to the server belongs to one of these categories:

* host data
* environment data (e.g. ENV, DISPLAY, etc)
* user data
* account data
* version data (app version, edition etc)
* configuration data (whatever)
* request data (details of an action performed by the user)
* variable data (any other details that are expected to change with each occurrence)


**Host tracking** is helpful to both Web and desktop developers, but in different ways. For desktop applications, host tracking allows you to group exceptions coming from the same user/machine without authenticating that user in any way. For Web developers, instance tracking helps identify excetions coming from the same server.

* `host.id` is an arbitrary sequence of characters identifying the particular host your application runs on; you are free to generate it whichever way you want (GUID is probably the best choice)
* `host.hostname` — if you want to provide it (note that host names change, on laptops they change often, so for a desktop application this is not a good candidate for an id)
* `host.*` — you can provide any other per-host data as you see fit (however chances are that the data you want to provide belongs to `configuration` instead).


**User tracking** should be employed if your application has user accounts, so that you can easily trace the exception back to the affected users.

* `user.id` is an arbitrary sequence of characters uniquely identifying the current/logged in user. A string prefixed with “anonymous” is treated specially
* `user.tags` (array of strings) — a list of arbitrary tags which you'd want to group on later (user groups, paid/free, current plan are all good candidates)
* `user.name` is the real/nickname of the logged in user.
* `user.email` is the email of the logged in user.
* `user.url` is a URL to show to your support stuff for the logged in user (could be a profile page, or a page for administering the user's account, etc).
* `user.*` any other per-user data



**Account tracking** is very similar to user tracking, and can be employed if you application has separate concepts of a user and an account. Note that while the user is expected to be changed by logging in, account might be calculated based on current subdomain, url or anything like that.

* `account.id` is an arbitrary sequence of characters uniquely identifying the account corresponding to the current action.
* `account.name` is a human-readable name of the account.
* `account.tags` (array of strings) — a list of arbitrary tags which you'd want to group on later (paid/free, current plan are all good candidates)
* `account.url` is a URL to show to your support stuff for the current account (e.g. an administrative page).
* `account.*` — any other per-account data


**Version tracking** helps you group occurrences by the version of your application.

* `version.number` is the version of the app as known by the user, e.g. '1.0rc2'.
* `version.revision` is SVN revision or Git/Mercurial commit id, if applicable
* `version.build` is a unique build id if applicable (should include version string if build ids are per-version)
* `version.edition` is an edition of your application (e.g. lite/standard/pro, home/professional/business/ultimate, etc)


**Configuration tracking** helps you group occurrences by the details of the currently running application instance. You can record any data here. For example:

* `configuration.port` might be a port number your application server is running on, in case you run multiple servers per host.


**Request tracking** helps group exceptions by the action performed by the user.

* `request.G_*`, `data.P_*`, `data.C_*` and `data.S_*` are recognized as GET, POST, cookie and session variables, and are rendered accordingly

* `env` contains information which is expected to stay the same for the given instance of your
application running on the given software configuration; many keys here are treated specially

* `stacktrace` contains the stack trace of the exception; this is an array which would only contain
a single element for many languages. Some languages (like Java) whose exceptions carry a linked
list of “causes” will want to provide several items here. 

* `stacktrace[0].kind` is exception kind/class/etc; exceptions with different values in this field
will be grouped into different bugs, so you don't want anything too variable here.
Even if you can only come up with several kinds (like JavaScript which has 3 kinds of exceptions),
you only want those kinds here.

* `stacktrace[0].message` is the message — not used for grouping, so can vary with each occurrence.

* `stacktrace[0].data.*` contains any additional exception-specific details

* `stacktrace[0].locations[0].absolute_file` — absolute file path on the local system.

* `stacktrace[0].locations[0].absolute_url` — an alternative to absolute_file in case your language runs in the browser (not sure this applies to anything but JavaScript).

* `stacktrace[0].locations[0].package` — installation-independent, platform-independent container for the class (e.g. a Java package; please don't pass anything if there's no such concept in your language).

* `stacktrace[0].locations[0].module` — installation-independent, platform-independent name for the file, which should identify that file more or less uniquely (e.g. a fully-qualified module name in Python).

* `stacktrace[0].locations[0].line` — 1-based line number, or 0 if not known.

* `stacktrace[0].locations[0].method` — method/function name, if any.

* `stacktrace[0].locations[0].class` — class name, if any.

* `stacktrace[0].locations[0].locals.*` — values of local variable

* `stacktrace[0].locations[0].source` — array of source lines, centered around the line of exception occurrence (must always have an odd length; an array of length *(2n+1)* has *n* lines of context before and after the current line)

* `ownership` - a JSON object, information about which paths, packages and modules belong to the application and which ones are the “outside world”; in each case, the first matching entry wins; any file not matched by any rule is considered to *not* belong to the application.

* `ownership.paths[0].absolute_path` — path to a file or directory.

* `ownership.paths[0].kind` — either `application` or `external` (external paths are all paths not belonging to your application).

* `ownership.packages[0].package_prefix` — e.g. “com.mycompany.myproject”.

* `ownership.packages[0].kind` — either `application` or `external`.

* `ownership.modules[0].module_prefix` — e.g. “myproject.myapp.views”.

* `ownership.modules[0].kind` — either `application` or `external`.


**Raw data.** If you just dump all environment variables, CrashKit will interpret certain CGI variables and set other data accordingly.

* `environment.variables` — a dictionary (JSON object) with keys and values corresponding to the environment variables of the process


If you are implementing a support for a new language,
please aim to keep the client-side processing to an absolute minimum.
Better let us know what you can collect and what you want to compute,
and we'll do our best to code it on the server.

Our goal is to do all possible processing on the server for two reasons:
first, data processing code changes more often than data collection code,
and if changing the server is sufficient to fix a bug / add a feature,
our customers don't have to bother updating their client code.
Second, with server-side processing it is possible to apply bug fixes
(and new features) retrospectively to the past reports.


Running Tests
-------------

There's a work-in-progress test suite which can be used to make sure all client modules work for you.

You need pexpect (http://www.noah.org/wiki/Pexpect, http://pexpect.sourceforge.net/pexpect.html):

    wget http://pexpect.sourceforge.net/pexpect-2.3.tar.gz
    tar xzf pexpect-2.3.tar.gz
    cd pexpect-2.3
    sudo python ./setup.py install

Run CrashKit development server on port 5005:

    dev_appserver.py -p 5005 crashkit/server
    
(oops, you don't have the source of the webapp — please wait a bit, I'll add support for running tests with the
live server soon; for now please just edit runtests.py and change CRASHKIT_HOST to “crashkitapp.appspot.com”).
    
Create a “test” account and add the following products if they don't already exists: py, django, php, java, js. (Should automate this step too.)

Run the tests:

    cd crashkit/tests
    ./runtests.py
