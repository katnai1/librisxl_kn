from collections import OrderedDict
import json


fpath = "ext-libris/src/main/resources/marcmap.json"
with open(fpath) as f:
    marcmap = json.load(f)

show_repeatable = True

entity_map = {
    'Books': 'Book',
    'Maps': 'Map',
    'Serials': 'Serial',
    'Computer': 'Digital',
    'ComputerFile': 'Digital'
}

fixprop_typerefs = {
    '007': [
        'mapMaterial',
        'computerMaterial',
        'globeMaterial',
        'tacMaterial',
        'projGraphMaterial',
        'microformMaterial',
        'nonProjMaterial',
        'motionPicMaterial',
        'kitMaterial',
        'notatedMusicMaterial',
        'remoteSensImageMaterial', #'soundKindOfMaterial',
        'soundMaterial',
        'textMaterial',
        'videoMaterial'
    ],
    '008': [
        'booksContents', #'booksItem', 'booksLiteraryForm' (subjectref), 'booksBiography',
        'computerTypeOfFile',
            #'mapsItem', #'mapsMaterial', # TODO: c.f. 007.mapMaterial
        #'mixedItem',
        #'musicItem', #'musicMatter', 'musicTransposition',
        'serialsTypeOfSerial', #'serialsItem',
        'visualMaterial', #'visualItem',
    ]
}

def to_name(name):
    name = name[0].upper() + name[1:]
    if name == 'Indexes':
        name = 'Index'
    elif name == 'Theses':
        name = 'Thesis'
    elif 'And' in name:
        name = 'Or'.join(s[0:-1] if s.endswith('s') else s
                for s in name.split('And'))
    elif name.endswith(('Atlas', 'Series')):
        pass
    elif name.endswith('ies'):
        name = name[0:-3] + 'y'
    elif name.endswith('s'):
        name = name[0:-1]
    return name


fixprops = marcmap['bib']['fixprops']
def fixprop_to_name(propname, key):
    dfn = fixprops[propname].get(key)
    if dfn and 'id' in dfn:
        return to_name(dfn['id'])
    else:
        return None

out = OrderedDict()

compositionTypeMap = out['compositionTypeMap'] = OrderedDict()
contentTypeMap = out['contentTypeMap'] = OrderedDict()
carrierTypeMap = out['carrierTypeMap'] = OrderedDict()

section = out['bib'] = OrderedDict()

prevtag, outf = None, None
for tag, field in sorted(marcmap['bib'].items()):
    if not tag.isdigit():
        continue
    prevf, outf = outf, OrderedDict()
    section[tag] = outf
    outf['entity'] = ""
    fixmaps = field.get('fixmaps')
    subfields = field.get('subfield')
    subtypes = None

    if fixmaps:
        tokenTypeMap = OrderedDict()
        for fixmap in fixmaps:
            content_type = None
            if len(fixmaps) > 1:
                outf['tokenTypeMap'] = tokenTypeMap
                type_name = fixmap.get('term') or fixmap['name'].split(tag + '_')[1]
                type_name = entity_map.get(type_name, type_name)
                fm = outf[type_name] = OrderedDict()

                if tag == '008':
                    for combo in fixmap['matchRecTypeBibLevel']:
                        rt, bl = combo
                        subtype_name = fixprop_to_name('typeOfRecord', rt)
                        if not subtype_name:
                            continue
                        comp_name = fixprop_to_name('bibLevel', bl)
                        if not comp_name:
                            continue

                        is_serial = type_name == 'Serial'
                        if comp_name == 'MonographItem' or is_serial:
                            content_type = contentTypeMap.setdefault(type_name, OrderedDict())
                            subtypes = content_type.setdefault('subclasses', OrderedDict())
                            if is_serial:
                                subtype_name += 'Serial'
                            typedef = subtypes[subtype_name] = OrderedDict()
                            typedef['typeOfRecord'] = rt
                        else:
                            comp_type = compositionTypeMap.setdefault(comp_name, OrderedDict())
                            comp_type['bibLevel'] = bl
                            parts = comp_type.setdefault('partRange', set())
                            parts.add(type_name)
                            parts.add(subtype_name)

                else:
                    for k in fixmap['matchKeys']:
                        tokenTypeMap[k] = type_name
            else:
                fm = outf

            for col in fixmap['columns']:
                off, length = col['offset'], col['length']
                key = (#str(off) if length == 1 else
                        '[%s:%s]' % (off, off+length))
                fm[key] = {'key': col.get('propRef', col.get('propId'))}
                dfn_key = col.get('propRef')

                if dfn_key in fixprop_typerefs['007']:
                    typemap = carrierTypeMap.setdefault(type_name, OrderedDict())
                elif dfn_key in fixprop_typerefs['008'] and content_type:
                    typemap = content_type.setdefault('formClasses', OrderedDict())
                else:
                    typemap = None

                if typemap is not None:
                    valuemap = fixprops[dfn_key]
                    for key, dfn in valuemap.items():
                        if key in ('_', '|'): continue
                        if 'id' in dfn:
                            v = dfn['id']
                            subname = to_name(v)
                        else:
                            subname = dfn['label_sv']
                        subdef = typemap[subname] = {dfn_key: key}

    elif not subfields:
        outf['key'] = field['id']

    else:
        for ind_key in ('ind1', 'ind2'):
            ind = field.get(ind_key)
            if not ind:
                continue
            ind_keys = sorted(k for k in ind if k != '_')
            if ind_keys:
                outf[ind_key.replace('ind', 'i')] = {}
        for code, subfield in subfields.items():
            sid = subfield.get('id') or ""
            if sid.endswith('Obsolete'):
                outf['obsolete'] = True
                sid = sid[0:-8]
            if (code, sid) in [('6', 'linkage'), ('8', 'fieldLinkAndSequenceNumber')]:
                continue
            subf = outf['$' + code] = OrderedDict()
            p_key = 'addProperty' if subfield.get('repeatable', False) else 'property'
            subf[p_key] = sid
        if len(outf.keys()) > 1 and outf == prevf:
            section[tag] = {'inherit': prevtag}
    if show_repeatable and 'repeatable' in field:
        outf['repeatable'] = field['repeatable']
    prevtag = tag


# sanity check..
prevranges = None
for k, v in compositionTypeMap.items():
    ranges = v['partRange']
    if prevranges and ranges - prevranges:
        print "differs", k
    else:
        assert any(r in contentTypeMap for r in ranges)
        v.pop('partRange')
    prevranges = ranges


class SetEncoder(json.JSONEncoder):
   def default(self, obj):
     return list(obj) if isinstance(obj, set) else super(SetEncoder, self).default(obj)


print json.dumps(out,
        cls=SetEncoder,
        indent=2,
        ensure_ascii=False,
        separators=(',', ': ')
        ).encode('utf-8')
