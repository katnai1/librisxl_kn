#!/usr/bin/env python
import sys, urllib, urllib2
try:
    from com.xhaus.jyson import JysonCodec as json
except ImportError:
    # From Python
    import json 


def transform(a_json, rtype):
    sug_json = {}
    link = None

    should_add = True

    for f in a_json['data']['fields']:
        for k, v in f.items():
            if k == '001':
                link = "/%s/%s" % (rtype, v)
            if k == '100':
                sug_json[k] = {}
                for sf in v['subfields']:
                    for sk, sv in sf.items():
                        if sk == '0' and rtype == 'bib':
                            should_add = False
                        sug_json[k][sk] = sv
            elif k in get_fields(rtype):
                f_list = sug_json.get(k, [])
                d = {}
                for sf in v['subfields']:
                    for sk, sv in sf.items():
                        d[sk] = sv
                f_list.append(d)
                sug_json[k] = f_list

    if sug_json.get('100', None) and should_add:
        f_100 = ' '.join(sug_json['100'].values())
        if link:
            sug_json['link'] = link
        sug_json['authorized'] = True if rtype == 'auth' else False
        sug_json = get_records(f_100, sug_json)
        
        return sug_json
    else:
        return 0
 
def get_fields(rtype):
    if rtype == 'auth':
        return ['400','500','678','856']
    return ['400','678']


def get_records(f_100, sug_json):
    try:
        url = 'http://libris.kb.se/xsearch'
        values = {'query' : 'forf:(%s) spr:swe' % f_100, 'format' : 'json'}

        data = urllib.urlencode(values)
        reply = urllib2.urlopen(url + "?" + data)

        response = reply.read().decode('utf-8')

        xresult = json.loads(response)['xsearch']

        sug_json['records'] = xresult['records']
        top_3 = xresult['list'][:3]
        top_titles = {}
        for p in top_3:
            top_titles[p['identifier']] = unicode(p['title'])
        sug_json['top_titles'] = top_titles

    except:
        0
    return sug_json

def record_type(leader):
    if list(leader)[6] == 'z':
        return "auth"
    return "bib"


_in_console = False
try:
    data = document.getDataAsString()
except:
    data = sys.stdin.read()
    whelk = None
    _in_console = True


#print "console mode", _in_console

in_json = json.loads(data)
rtype = record_type(in_json['data']['leader'])
suggest_source = rtype 
if (rtype == 'bib'):
    suggest_source = 'name'

w_name = whelk.name if whelk else "test"

identifier = "/%s/%s/%s" % (w_name, suggest_source, document.identifier.toString().split("/")[-1])
sug_json = transform(in_json, rtype)
if sug_json:
    sug_json['identifier'] = identifier
    r = json.dumps(sug_json)

    if whelk:
        mydoc = whelk.createDocument().withIdentifier(identifier).withData(r).withContentType("application/json")

        #print "Sparar dokument i whelken daaraa"
        uri = whelk.store(mydoc)
else:
    print "Record %s has no usable auth information. (i.e. no 100-field)" % document.identifier

if _in_console:
    print r
