# REST API

The following is autogenerated from the OpenAPI specification
[`openapi.json`](openapi.json) and included in the product as fully interactive
documentation. When running Quine, you can issue API calls directly from the embedded documentation page.

<div id="swagger-ui"></div>
<script src="https://unpkg.com/swagger-ui-dist@3.51.2/swagger-ui-bundle.js" charset="UTF-8"></script>
<link rel="stylesheet" href="https://unpkg.com/swagger-ui-dist@3.51.2/swagger-ui.css" type="text/css" />
<style type="text/css">
.scheme-container {
  margin: 0 !important;
  padding: 0 !important;
}
</style>
<script type="text/javascript">
var sectionOrder = [
  "Standing Queries",
  "Ingest Streams",
  "Cypher Query Language",
  "Gremlin Query Language",
  "Literal Node Operations",
  "UI Styling",
  "Administration"
]
var ui = SwaggerUIBundle({
    url: "openapi.json",
    dom_id: '#swagger-ui',
    syntaxHighlight: false, // it conflicts with other styling in Paradox
    presets: [SwaggerUIBundle.presets.apis],
    tagsSorter: function(tag1, tag2) {
      return sectionOrder.indexOf(tag1) - sectionOrder.indexOf(tag2);
    },
    plugins: [
      function() {
        return {
          wrapComponents: {
            authorizeBtn: () => () => null // remove the "Authorize" button
          },
          statePlugins: {
            spec: {
              wrapSelectors: {
                allowTryItOutFor: () => () => false // remove the "Try it out" buttons
              }
            }
          },
          components: {
            info: () => null // remove the entire info header
          }
        }
      }
    ]
  })
</script>