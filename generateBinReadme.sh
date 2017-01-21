#!/usr/bin/env bash
>&2 echo "NOTE: Console width should be 100 characters wide"

monarchHelp=$(monarch -?)
applyHelp=$(monarch apply -?)
setHelp=$(monarch set -?)

echo "
\`\`\`
$monarchHelp
\`\`\`

\`\`\`
$applyHelp
\`\`\`

\`\`\`
$setHelp
\`\`\`
"
