# svg-element-template-converter
Takes SVGs and converts them for use in Camunda Modeler Element Templates

The main class is src/main/java/org/vg/utilities/RescaleImage

Look to svgs/Run_Commands.txt for sample arguments to pass. It basically goes:

"SVG input file location" "Modeler Element Template file location" "Icon width in pixels" "Element Template ID" "Element Template Name/Description/etc" "What BPMN elements it will apply to"

The animated SVG sample will currently need to be manually hacked to get it to work. The converter will not covert it properly. Use at your own risk!

Goal is to create a stable of SVG icons for the community and have a converter for folks to create their own.
