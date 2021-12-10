import whelk.util.DocumentUtil

PrintWriter removed = getReportWriter('removed.tsv')
PrintWriter exceptions = getReportWriter('exceptions.tsv')

selectByCollection('bib') { data ->
    Map instance = data.graph[1]
    String id = data.doc.shortId

    List badPaths = []

    // Remove broken links
    boolean modified = DocumentUtil.traverse(instance) { value, path ->
        if (value in String && value.startsWith('http://www.scb.se/')) {
            try {
                URLConnection conn = new URL(value).openConnection()

                if (conn.getResponseCode() in [301, 302])
                    conn = new URL(conn.getHeaderField('Location')).openConnection()

                int responseCode = conn.getResponseCode()

                if (responseCode in [400, 404]) {
                    badPaths << path
                    removed.println("${id}\t${responseCode}\t${path}\t${value}")
                    return new DocumentUtil.Remove()
                }
            } catch (Exception ex) {
                exceptions.println("${id}\t${ex}")
            }
        }
    }

    List containingObjectPaths = badPaths.findResults { bp ->
        try {
            return getContainingObjectPath(instance, bp)
        } catch (NewInPathException ex) {
            exceptions.println("${id}\t${ex}")
            return null
        }
    }.unique()

    // Remove the objects containing broken links altogether
    containingObjectPaths.reverseEach { p ->
        try {
            Object removedObj = removeContainingObject(instance, p)
            incrementStats("removed", removedObj)
            removeAllEmpty(instance, p)
        } catch (AlreadyRemovedException ex) {
            exceptions.println("${id}\t${ex}")
        } catch (NonEntityException ex) {
            exceptions.println("${id}\t${ex}")
        }
    }

    if (modified) {
        data.scheduleSave()
    }
}

List getContainingObjectPath(Object item, List path) {
    List containingPath = []

    for (p in path) {
        if (item[p] == null) {
            return containingPath
        } else {
            containingPath << p
            item = item[p]
        }
    }

    throw new NewInPathException("New object found at path ${path}")
}

Object removeContainingObject(Object item, List path) {
    for (p in path[0..<-1]) {
        item = item[p]
        if (item == null)
            throw new AlreadyRemovedException("Object at path ${path} already removed")
    }

    Object removedObj = item.remove(path[-1])

    if (!(removedObj in Map && removedObj.containsKey('@type')))
        throw new NonEntityException("Object at path ${path} doesn't seem to be an entity")

    return removedObj
}

void removeAllEmpty(Object item, List path) {
    boolean removedEmpty = removeEmpty(item, path)
    while (removedEmpty) {
        removedEmpty = removeEmpty(item, path)
    }
}

boolean removeEmpty(Object item, List path) {
    for (p in path) {
        if (item[p] == null) {
            return false
        } else if (item[p].isEmpty()) {
            item.remove(p)
            return true
        } else {
            item = item[p]
        }
    }
}

class NewInPathException extends Exception {
    NewInPathException(String msg) { super(msg) }
}

class AlreadyRemovedException extends Exception {
    AlreadyRemovedException(String msg) { super(msg) }
}

class NonEntityException extends Exception {
    NonEntityException(String msg) { super(msg) }
}