import whelk.Whelk

PrintWriter failedUpdates = getReportWriter("failed-updates")
PrintWriter scheduledForDelete = getReportWriter("scheduled-for-delete")
PrintWriter scheduledForRelinking = getReportWriter("scheduled-for-relinking")
PrintWriter scheduledForUpdate = getReportWriter("scheduled-for-update")
File bibids = new File('INPUT')

/*
 1. Find the preffered URI of the replacing document
 2. Find everything linking to the disappearing record, relink it to the replacement
 3. Get all IDs from the disappearing record, add them as sameAs-IDs on the replacement
 4. Remove the disappearing record
 */

for (String job : bibids.readLines()) {

    String[] parts = job.split(" ")
    if (parts.length != 2) {
        System.err.println("Not exactly 2 strings separated by a space in: " + job)
        continue
    }
    String idToReplaceWith = parts[0].trim()
    String idToBeReplaced = parts[1].trim()
    String preferedUriToReplaceWith = null

    // Find preferred URI
    selectBySqlWhere("id = '$idToReplaceWith'", silent: true, { item ->
        List<String> recordIDs = item.doc.getRecordIdentifiers()
        List<String> thingIDs = item.doc.getThingIdentifiers()
        if (!thingIDs.isEmpty()) {
            preferedUriToReplaceWith = thingIDs[0]
        } else if (!recordIDs.isEmpty()) {
            preferedUriToReplaceWith = recordIDs[0]
        }
    })
    if (preferedUriToReplaceWith == null) {
        System.err.println("Could not find preferred URI to use in: " + job)
        continue
    }

    // Find disappearing URIs
    List<String> disappearingRecordIDs
    List<String> disappearingThingIDs
    selectBySqlWhere("id = '$idToBeReplaced'", silent: true, { item ->
        disappearingRecordIDs = item.doc.getRecordIdentifiers()
        disappearingThingIDs = item.doc.getThingIdentifiers()
    })

    // Replace links in dependers, to the preferred URI
    selectBySqlWhere("id in (select id from lddb__dependencies where dependsonid = '$idToBeReplaced')", silent: true, { item ->
        List<String> linksToReplace = []
        linksToReplace.addAll(disappearingRecordIDs)
        linksToReplace.addAll(disappearingThingIDs)

        replaceLinks(item.graph, item.whelk, preferedUriToReplaceWith, linksToReplace)

        scheduledForRelinking.println("${item.doc.getURI()}")
        item.scheduleSave(onError: { e ->
            failedUpdates.println("Failed to update ${item.doc.shortId} due to: $e")
        })
    })

    selectBySqlWhere("id = '$idToBeReplaced'", silent: true, { item ->
        scheduledForDelete.println("${item.doc.getURI()}")
        item.scheduleDelete(onError: { e ->
            failedUpdates.println("Failed to remove ${item.doc.shortId} due to: $e")
        })
    })

    // Add disappearing URIs as sameAs
    selectBySqlWhere("id = '$idToReplaceWith'", silent: true, { item ->
        for (String id : disappearingRecordIDs)
            item.doc.addRecordIdentifier(id)
        for (String id : disappearingThingIDs)
            item.doc.addThingIdentifier(id)

        scheduledForUpdate.println("${item.doc.getURI()}")
        item.scheduleSave(onError: { e ->
            failedUpdates.println("Failed to update ${item.doc.shortId} due to: $e")
        })
    })
}

boolean replaceLinks(Object node, Whelk whelk, String newLinkTarget, List<String> linksToReplace) {
    if (node instanceof Map) {
        Map map = node

        if (map.size() == 1 && linksToReplace.contains(map["@id"]) ) {
            map["@id"] = newLinkTarget
        }

        for (String key : map.keySet()) {
            replaceLinks(map[key], whelk, newLinkTarget, linksToReplace)
        }
    }

    if (node instanceof List) {
        List list = node
        for (Object e : list) {
            replaceLinks(e, whelk, newLinkTarget, linksToReplace)
        }
    }
}
