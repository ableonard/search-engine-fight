@Grab(group='org.ccil.cowan.tagsoup', module='tagsoup', version='1.2.1' )

class SearchEngineFight {
    String query
    String bingURI = "http://m.bing.com/search?q="
    String yahooURI = "http://m.yahoo.com/search?p="
    String googleURI = "https://www.google.com/search?q="
    String userAgent = "Mozilla/4.0 (compatible; MSIE 6.0; Windows CE; IEMobile 6.12; Microsoft ZuneHD 4.3)"

    SearchComparator(String query) {
        this.query = query;
    }

    Map<String, List<Result>> getResults() {
        return [Bing: getBingResults(), Yahoo: getYahooResults(), Google: getGoogleResults()]
    }

    List<Result> getBingResults() {
        def document
        try {
            document = getDocumentForBaseURI(bingURI)
        } catch (IOException e) {
            return [new Result('Unable to retrieve Bing results at this time', '', '')]
        }

        def linkBlocks = document.'**'.find { it.name() == 'ul' && it.@class == 'ansGrp ansVGrp' }
        return linkBlocks.li.collect { b ->
            def title = b.'**'.find { it.name() == 'h5' && it.@class == 'ansTtl' }
            def desc = b.'**'.find { it.name() == 'p' && it.@class == 'ansDesc' }
            def link = b.'**'.find { it.name() == 'div' && it.@class == 'srcLnk' }
            new Result(title?.text(), desc?.text(), link?.text())
        }
    }

    List<Result> getYahooResults() {
        def document
        try {
            document = getDocumentForBaseURI(yahooURI)
        } catch (IOException e) {
            return [new Result('Unable to retrieve Yahoo results at this time', '', '')]
        }

        def resultsBlock = document.body.'div'.'div'[1].'div'[0].'div'[1]
        def linkBlocks = resultsBlock.'*'.findAll { it.name() == 'div' && it.@class =~ 'uip.*' }

        return linkBlocks.collect { b ->
            def title = b.'**'.find { it.name() == 'div' && it.@class == 'uic link first' }
            def desc = b.'**'.find { it.name() == 'div' && it.@class == 'uic small' }
            def link = b.'**'.find { it.name() == 'div' && it.@class == 'uic url small last'}
            new Result(title?.text(), desc?.text(), link?.text())
        }
    }

    List<Result> getGoogleResults() {
        def document
        try {
            document = getDocumentForBaseURI(googleURI)
        } catch (IOException e) {
            return [new Result('Unable to retrieve Google results at this time', '', '')]
        }

        def linkBlocks = document.'**'.findAll { it.name() == 'div' && it.@class == 'web_result' }
        return linkBlocks.collect { b ->
            def title = b.'div'[0].a.text()
            def desc = b.'div'[1].text()
            def link = b.'div'[1].'div'[1].span.text()
            new Result(title, desc[0..desc.size() - link.size() - 1], link)
        }
    }

    def getDocumentForBaseURI(String baseURI) {
        URLConnection connection = new URL(baseURI + URLEncoder.encode(query, "UTF-8")).openConnection()
        connection.setRequestProperty("User-Agent", userAgent)
        return new XmlSlurper(new org.ccil.cowan.tagsoup.Parser()).parse(connection.getContent())
    }

    private class Result {
        String title
        String desc
        String URI

        Result(String title, String desc, String URI) {
            this.title = title
            this.desc = desc
            this.URI = URI
        }
    }
}

s = new SearchEngineFight("example")

results = s.getResults()

order = ["Bing", "Yahoo", "Google"]
Collections.shuffle(order)

order.eachWithIndex { name, i ->
    println "----------------------"
    println "Search Engine ${i + 1}"
    println "----------------------"
    results[name].each { result ->
        println "${result.title}\n"
        println "  ${result.desc}\n"
        println "  ${result.URI}"
        println "\n"
    }
}

println "Engine 1 was ${order[0]}, Engine 2 was ${order[1]}, and Engine 3 was ${order[2]}"
