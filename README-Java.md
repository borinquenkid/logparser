Apache HTTPD logparser
===
This is a Logparsing framework intended to make parsing Apache HTTPD logfiles much easier.

The basic idea is that you should be able to have a parser that you can construct by simply 
telling it with what configuration options the line was written.

Usage (Java)
===
For the Java API there is an annotation based parser.

I assume we have a logformat variable that looks something like this:

    String logformat = "%h %l %u %t \"%r\" %>s %b \"%{Referer}i\" \"%{User-Agent}i\"";

**Step 1: What CAN we get from this line?**

To figure out what values we CAN get from this line we instantiate the parser with a dummy class
that does not have ANY @Field annotations. The "Object" class will do just fine for this purpose.

    Parser<Object> dummyParser = new ApacheHttpdLoglineParser<Object>(Object.class, logformat);
    List<String> possiblePaths = dummyParser.getPossiblePaths();
    for (String path: possiblePaths) {
        System.out.println(path);
    }

You will get a list that looks something like this:

    IP:connection.client.host
    NUMBER:connection.client.logname
    STRING:connection.client.user
    TIME.STAMP:request.receive.time
    TIME.DAY:request.receive.time.day
    TIME.MONTHNAME:request.receive.time.monthname
    TIME.MONTH:request.receive.time.month
    TIME.YEAR:request.receive.time.year
    TIME.HOUR:request.receive.time.hour
    TIME.MINUTE:request.receive.time.minute
    TIME.SECOND:request.receive.time.second
    TIME.MILLISECOND:request.receive.time.millisecond
    TIME.ZONE:request.receive.time.timezone
    HTTP.FIRSTLINE:request.firstline
    HTTP.METHOD:request.firstline.method
    HTTP.URI:request.firstline.uri
    HTTP.QUERYSTRING:request.firstline.uri.query
    STRING:request.firstline.uri.query.*
    HTTP.PROTOCOL:request.firstline.protocol
    HTTP.PROTOCOL.VERSION:request.firstline.protocol.version
    STRING:request.status.last
    BYTES:response.body.bytesclf
    HTTP.URI:request.referer
    HTTP.QUERYSTRING:request.referer.query
    STRING:request.referer.query.*
    HTTP.USERAGENT:request.user-agent

Now some of these lines contain a * . 
This is a wildcard that can be replaced with any 'name'.

**Step 2 Create the receiving POJO** 

We need to create the receiving record class that is simply a POJO that does not need any interface or inheritance. 
In this class we create setters that will be called when the specified field has been found in the line.

So we can now add to this class a setter that simply receives a single value: 

    @Field("IP:connection.client.host")
    public void setIP(final String value) {
        ip = value;
    }

If we really want the name of the field we can also do this

    @Field("STRING:request.firstline.uri.query.img")
    public void setQueryImg(final String name, final String value) {
        results.put(name, value);
    }

This latter form is very handy because this way we can obtain all values for a wildcard field

    @Field("STRING:request.firstline.uri.query.*")
    public void setQueryStringValues(final String name, final String value) {
        results.put(name, value);
    }

Or a combination of the above examples where you specify multiple field patterns

    @Field({"IP:connection.client.host", 
            "STRING:request.firstline.uri.query.*"})
    public void setValue(final String name, final String value) {
        results.put(name, value);
    }

**Step 3 Use the parser in your application.**

You create an instance of the parser

        Parser<MyRecord> parser = new ApacheHttpdLoglineParser<MyRecord>(MyRecord.class, logformat);

And then call the parse method repeatedly for each line.
You can do this like this (for each call a new instance of "MyRecord" is instantiated !!):

        MyRecord record = parser.parse(logline);
 
Or you can call it like this:
Only once:

        MyRecord record = new MyRecord(); 

And then for each logline:

        record.clear(); // Which is up to you to implement to 'reset' the record to it's initial state.
        parser.parse(record, logline);

Notes about the setters

- Only if a value exists in the actual logline the setter will be called (mainly relevant if you want to get a specific query param or cookie).
- If you specifiy the same field on several setters then each of these setters will be called.

Have a look at the 'httpdlog-testclient' for a working example.

Usage (PIG)
===
You simply register the httpdlog-pigloader-1.0-SNAPSHOT-job.jar

    REGISTER target/httpdlog-pigloader-1.0-SNAPSHOT-job.jar
    
And then call the loader with a dummy file (must exist, won't be read) and the parameter called 'fields'. This will return a list of all possible fields. Note that weher a '*' appears this means there are many possible values that can appear there (for example the keys of a query string in a URL).
As you can see there is a kinda sloppy type mechanism to stear the parsing, don't change that as the persing really relies on this.
    
    Fields = 
      LOAD 'test.pig' -- Any file as long as it exists 
      USING nl.basjes.pig.input.apachehttpdlog.Loader(
        '%h %l %u %t "%r" %>s %b "%{Referer}i" "%{User-Agent}i"',
        'Fields' ) AS (fields);
    
    DUMP Fields;
   
The output of this command is this:
Clicks =
    LOAD 'access.log'
    USING nl.basjes.pig.input.apachehttpdlog.Loader(
    '%h %l %u %t "%r" %>s %b "%{Referer}i" "%{User-Agent}i"',

        'IP:connection.client.host',
        'NUMBER:connection.client.logname',
        'STRING:connection.client.user',   
        'TIME.STAMP:request.receive.time', 
        'TIME.DAY:request.receive.time.day',
        'TIME.MONTHNAME:request.receive.time.monthname',
        'TIME.MONTH:request.receive.time.month',        
        'TIME.WEEK:request.receive.time.weekofweekyear',
        'TIME.YEAR:request.receive.time.weekyear',      
        'TIME.YEAR:request.receive.time.year',          
        'TIME.HOUR:request.receive.time.hour',          
        'TIME.MINUTE:request.receive.time.minute',      
        'TIME.SECOND:request.receive.time.second',      
        'TIME.MILLISECOND:request.receive.time.millisecond',
        'TIME.ZONE:request.receive.time.timezone',          
        'TIME.EPOCH:request.receive.time.epoch',            
        'HTTP.FIRSTLINE:request.firstline',                 
        'HTTP.METHOD:request.firstline.method',             
        'HTTP.URI:request.firstline.uri',                   
        'HTTP.PROTOCOL:request.firstline.uri.protocol',     
        'HTTP.USERINFO:request.firstline.uri.userinfo',     
        'HTTP.HOST:request.firstline.uri.host',             
        'HTTP.PORT:request.firstline.uri.port',             
        'HTTP.PATH:request.firstline.uri.path',             
        'HTTP.QUERYSTRING:request.firstline.uri.query',     
        'STRING:request.firstline.uri.query.*',         -- You cannot put a * here yet. You MUST specify a specific field.',
        'HTTP.REF:request.firstline.uri.ref',                                                                               
        'HTTP.PROTOCOL:request.firstline.protocol',                                                                         
        'HTTP.PROTOCOL.VERSION:request.firstline.protocol.version',                                                         
        'STRING:request.status.last',                                                                                       
        'BYTES:response.body.bytesclf',                                                                                     
        'HTTP.URI:request.referer',                                                                                         
        'HTTP.PROTOCOL:request.referer.protocol',
        'HTTP.USERINFO:request.referer.userinfo',
        'HTTP.HOST:request.referer.host',
        'HTTP.PORT:request.referer.port',
        'HTTP.PATH:request.referer.path',
        'HTTP.QUERYSTRING:request.referer.query',
        'STRING:request.referer.query.*',       -- You cannot put a * here yet. You MUST specify a specific field.',
        'HTTP.REF:request.referer.ref',
        'HTTP.USERAGENT:request.user-agent')
    AS (
        connection_client_host:chararray,
        connection_client_logname:long,
        connection_client_user:chararray,
        request_receive_time:chararray,
        request_receive_time_day:long,
        request_receive_time_monthname:chararray,
        request_receive_time_month:long,
        request_receive_time_weekofweekyear:long,
        request_receive_time_weekyear:long,
        request_receive_time_year:long,
        request_receive_time_hour:long,
        request_receive_time_minute:long,
        request_receive_time_second:long,
        request_receive_time_millisecond:long,
        request_receive_time_timezone:chararray,
        request_receive_time_epoch:long,
        request_firstline:chararray,
        request_firstline_method:chararray,
        request_firstline_uri:chararray,
        request_firstline_uri_protocol:chararray,
        request_firstline_uri_userinfo:chararray,
        request_firstline_uri_host:chararray,
        request_firstline_uri_port:long,
        request_firstline_uri_path:chararray,
        request_firstline_uri_query:chararray,
        request_firstline_uri_query_*:chararray,        -- You cannot put a * here yet. You MUST specify name.,
        request_firstline_uri_ref:chararray,
        request_firstline_protocol:chararray,
        request_firstline_protocol_version:chararray,
        request_status_last:chararray,
        response_body_bytesclf:long,
        request_referer:chararray,
        request_referer_protocol:chararray,
        request_referer_userinfo:chararray,
        request_referer_host:chararray,
        request_referer_port:long,
        request_referer_path:chararray,
        request_referer_query:chararray,
        request_referer_query_*:chararray,      -- You cannot put a * here yet. You MUST specify name.,
        request_referer_ref:chararray,
        request_user-agent:chararray);

As you can see most values are 'chararray' but some are of type 'long' or 'double'.
Also note that some of the lines have a comment that you must make a choice before you can proceed.
Now that we have all the possible values that CAN be produced from this logformat we simply choose the ones we need and tell the Loader we want those.
    
    Clicks = 
      LOAD 'access_log.gz' 
      USING nl.basjes.pig.input.apachehttpdlog.Loader(
        '%h %l %u %t "%r" %>s %b "%{Referer}i" "%{User-Agent}i"',
        'IP:connection.client.host',
        'HTTP.URI:request.firstline.uri',
        'STRING:request.firstline.uri.query.foo',
        'STRING:request.status.last',
        'HTTP.URI:request.referer',
        'STRING:request.referer.query.foo',
        'HTTP.USERAGENT:request.user-agent')
    
        AS ( 
        ConnectionClientHost:chararray,
        RequestFirstlineUri:chararray,
        RequestFirstlineUriQueryFoo:chararray,
        RequestStatusLast:chararray,
        ResponseBodyBytesclf:long,
        RequestReferer:chararray,
        RequestRefererQueryFoo:chararray,
        RequestUseragent:chararray);
    
From here you can do as you want with the resulting tuples. Note that almost everything is output as a chararray, yet things that seem like number (based on the sloppy typing) are output as longs.

License
===
This software is licenced under GPLv3. If you want to include this in a commercial (non-opensource) product then simply contact me and we'll talk about this.