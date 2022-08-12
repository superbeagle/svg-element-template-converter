package org.svg.utilities;

import org.apache.batik.anim.dom.SAXSVGDocumentFactory;
import org.apache.batik.anim.dom.SVGDOMImplementation;
import org.apache.batik.parser.PathParser;
import org.apache.batik.parser.PointsParser;
import org.apache.batik.util.XMLResourceDescriptor;

import org.json.JSONArray;
import org.w3c.dom.*;
import org.w3c.dom.svg.SVGDocument;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.*;
import java.util.HashMap;
import java.util.Iterator;
import java.util.ListIterator;

import org.apache.commons.io.FileUtils;
import org.json.JSONObject;


public class RescaleImage {

    public static void main(String[] args) {

        try {
            String parser = XMLResourceDescriptor.getXMLParserClassName();
            SAXSVGDocumentFactory f = new SAXSVGDocumentFactory(parser);

            String uri = args[0];
            String elementTemplateLocation = args[1];
            String targetWidth = args[2];
            //String targetHeight = args[2];
            String templateId = args[3];
            String templateName = args[4];
            String appliesTo = args[5];
            boolean templateMatch = false;

            SVGDocument svg = f.createSVGDocument(uri);

            DOMImplementation impl = SVGDOMImplementation.getDOMImplementation();
            String svgNS = SVGDOMImplementation.SVG_NAMESPACE_URI;
            Document doc = impl.createDocument(svgNS, "svg", null);

            // Get the root element (the 'svg' element).
            Element svgRoot = doc.getDocumentElement();

            // get width and height of original svg viewbox
            NamedNodeMap nnmRoot = svg.getRootElement().getAttributes();
            String[] vbElements = nnmRoot.getNamedItem("viewBox").getNodeValue().split(" ");
            String originalWidth = vbElements[2];
            String originalHeight = vbElements[3];
            Float calcTargetHeight = (Float.valueOf(targetWidth)/Float.valueOf(originalWidth) * Float.valueOf(originalHeight));
            String targetHeight = calcTargetHeight.toString();

            //System.out.println("viewBox is "+ nnmRoot.getNamedItem("viewBox").getNodeValue());

            // Set the width and height attributes on the root 'svg' element.
            svgRoot.setAttributeNS(null, "width", targetWidth);
            svgRoot.setAttributeNS(null, "height", targetHeight);
            svgRoot.setAttributeNS(null, "viewbox", "0 0 "+targetWidth+" "+targetHeight);

            NodeList nl = svg.getElementsByTagName("style");
            for (int i=0; i < nl.getLength(); i++) {
                Element style = doc.createElementNS(svgNS, "style");
                NamedNodeMap nnm = nl.item(i).getAttributes();
                if (nnm.getNamedItem("type") != null) {
                    style.setAttribute("type", nnm.getNamedItem("type").getTextContent());
                    style.setTextContent(nl.item(i).getTextContent());
                }
                svgRoot.appendChild(style);
            }

            nl = svg.getElementsByTagName("circle");
            for (int i=0; i < nl.getLength(); i++) {
                Element circle = doc.createElementNS(svgNS, "circle");
                NamedNodeMap nnm = nl.item(i).getAttributes();
                if (nnm.getNamedItem("cx") != null) {
                    //System.out.println("Found circle x" +nnm.getNamedItem("cx").getTextContent());
                    Float x = Float.valueOf(nnm.getNamedItem("cx").getTextContent());
                    Float newX = Float.valueOf(targetWidth)/Float.valueOf(originalWidth) * x;
                    circle.setAttribute("cx", newX.toString());
                }

                if (nnm.getNamedItem("cy") != null) {
                    //System.out.println("Found circle y" +nnm.getNamedItem("cy").getTextContent());
                    Float y = Float.valueOf(nnm.getNamedItem("cy").getTextContent());
                    Float newY = Float.valueOf(targetHeight)/Float.valueOf(originalHeight) * y;
                    circle.setAttribute("cy", newY.toString());
                }

                if (nnm.getNamedItem("r") != null) {
                    //System.out.println("Found circle r");
                    Float r = Float.valueOf(nnm.getNamedItem("r").getTextContent());
                    Float newR = Float.valueOf(targetHeight)/Float.valueOf(originalHeight) * r;
                    circle.setAttribute("r", newR.toString());
                }
                /*System.out.println("circle is "+circle.getAttribute("cx"));
                System.out.println("circle is "+circle.getAttribute("cy"));
                System.out.println("circle is "+circle.getAttribute("r"));*/
                svgRoot.appendChild(circle);
            }

            nl = svg.getElementsByTagName("g");
            for (int i=0; i < nl.getLength(); i++) {

            }

            nl = svg.getElementsByTagName("path");
            for (int i=0; i < nl.getLength(); i++) {
                NamedNodeMap nnm = nl.item(i).getAttributes();
                Node node = nnm.getNamedItem("d");
                //System.out.println("i is "+i);

                PathParser pp = new PathParser();
                MyPathHandler mph = new MyPathHandler(Float.valueOf(originalWidth), Float.valueOf(originalHeight) , Float.valueOf(targetWidth), Float.valueOf(targetHeight));
                pp.setPathHandler(mph);
                pp.parse(node.getTextContent());

                Element path = doc.createElementNS(svgNS, "path");
                path.setAttributeNS(null, "d",mph.getPath());
                if (nnm.getNamedItem("style") != null) {
                    path.setAttribute("style", nnm.getNamedItem("style").getTextContent());
                }
                if (nnm.getNamedItem("fill") != null) {
                    path.setAttribute("fill", nnm.getNamedItem("fill").getTextContent());
                }

                if (nnm.getNamedItem("id") != null) {
                    path.setAttribute("id", nnm.getNamedItem("id").getTextContent());
                }

                if (nnm.getNamedItem("class") != null) {
                    path.setAttribute("class", nnm.getNamedItem("class").getTextContent());
                }
                svgRoot.appendChild(path);

                StringWriter sw = new StringWriter();
                TransformerFactory tf = TransformerFactory.newInstance();
                Transformer transformer = tf.newTransformer();
                transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
                transformer.setOutputProperty(OutputKeys.METHOD, "xml");
                transformer.setOutputProperty(OutputKeys.INDENT, "yes");
                transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
                transformer.transform(new DOMSource(path), new StreamResult(sw));

                //System.out.println("unescaped path is"+sw);
                String pathEscaped = sw.toString().replace("<", "%3C");
                pathEscaped = pathEscaped.replace(">", "%3E");
                pathEscaped = pathEscaped.replace("\"", "'");
                pathEscaped = pathEscaped.replace("#", "%23");
                pathEscaped = "data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='"+targetWidth+"' height='"+targetHeight+"' viewBox='0 0 "+targetWidth+" "+targetHeight+"' "+ pathEscaped + "%3C/svg%3E";
                //System.out.println("escaped path is "+ pathEscaped);

            }

            nl = svg.getElementsByTagName("polygon");
            for (int i=0; i < nl.getLength(); i++) {
                NamedNodeMap nnm = nl.item(i).getAttributes();
                Node node = nnm.getNamedItem("points");

                PointsParser pp = new PointsParser();
                MyPointsHandler mph = new MyPointsHandler(Float.valueOf(originalWidth), Float.valueOf(originalHeight) , Float.valueOf(targetWidth), Float.valueOf(targetHeight));
                pp.setPointsHandler(mph);
                pp.parse(node.getTextContent());

                Element polygon = doc.createElementNS(svgNS, "polygon");
                polygon.setAttributeNS(null, "points",mph.getPoints());

                svgRoot.appendChild(polygon);

                StringWriter sw = new StringWriter();
                TransformerFactory tf = TransformerFactory.newInstance();
                Transformer transformer = tf.newTransformer();
                transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
                transformer.setOutputProperty(OutputKeys.METHOD, "xml");
                transformer.setOutputProperty(OutputKeys.INDENT, "yes");
                transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
                transformer.transform(new DOMSource(polygon), new StreamResult(sw));


                String polygonEscaped = sw.toString().replace("<", "%3C");
                polygonEscaped = polygonEscaped.replace(">", "%3E");
                polygonEscaped = polygonEscaped.replace("\"", "'");
                polygonEscaped = polygonEscaped.replace("#", "%23");
                polygonEscaped = "data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='"+targetWidth+"' height='"+targetHeight+"' viewBox='0 0 "+targetWidth+" "+targetHeight+"' "+ polygonEscaped + "%3C/svg%3E";
            }

            nl = svg.getElementsByTagName("rect");

            for (int i=0; i < nl.getLength(); i++) {
                Element rect = doc.createElementNS(svgNS, "rect");
                NamedNodeMap nnm = nl.item(i).getAttributes();
                if (nnm.getNamedItem("x") != null) {
                    System.out.println("Found rect x " +nnm.getNamedItem("x").getTextContent());
                    Float x = Float.valueOf(nnm.getNamedItem("x").getTextContent());
                    Float newX = Float.valueOf(targetWidth)/Float.valueOf(originalWidth) * x;
                    rect.setAttribute("x", newX.toString());
                }

                if (nnm.getNamedItem("y") != null) {
                    System.out.println("Found rect y " +nnm.getNamedItem("y").getTextContent());
                    Float y = Float.valueOf(nnm.getNamedItem("y").getTextContent());
                    Float newY = Float.valueOf(targetHeight)/Float.valueOf(originalHeight) * y;
                    rect.setAttribute("y", newY.toString());
                }

                if (nnm.getNamedItem("width") != null) {
                    System.out.println("Found rect width "+nnm.getNamedItem("width").getTextContent());
                    Float width = Float.valueOf(nnm.getNamedItem("width").getTextContent());
                    Float newWidth = Float.valueOf(targetWidth)/Float.valueOf(originalWidth) * width;
                    rect.setAttribute("width", newWidth.toString());
                }

                if (nnm.getNamedItem("height") != null) {
                    System.out.println("Found rect height "+nnm.getNamedItem("height").getTextContent());
                    Float height = Float.valueOf(nnm.getNamedItem("height").getTextContent());
                    Float newHeight = Float.valueOf(targetHeight)/Float.valueOf(originalHeight) * height;
                    rect.setAttribute("height", newHeight.toString());
                }

                if (nnm.getNamedItem("class") != null) {
                    rect.setAttribute("class", nnm.getNamedItem("class").getTextContent());
                }
                System.out.println("rect x is "+rect.getAttribute("x"));
                System.out.println("rect y is "+rect.getAttribute("y"));
                System.out.println("rect width is "+rect.getAttribute("width"));
                System.out.println("rect height is "+rect.getAttribute("height"));
                svgRoot.appendChild(rect);
            }

            System.out.println("Rectangles found "+ nl.getLength() );

            StringWriter sw = new StringWriter();
            TransformerFactory tf = TransformerFactory.newInstance();
            Transformer transformer = tf.newTransformer();
            transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
            transformer.setOutputProperty(OutputKeys.METHOD, "xml");
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
            transformer.transform(new DOMSource(doc), new StreamResult(sw));
            System.out.println("svg is " + sw.toString());

            StringBuilder sb = new StringBuilder("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='"+targetWidth+"' height='"+targetHeight+"' viewBox='0 0 "+targetWidth+" "+targetHeight+"' %3E");

            NodeList styles = doc.getElementsByTagName("style");
            for(int i=0; i < styles.getLength(); i++) {
                StringWriter writer = new StringWriter();
                transformer = TransformerFactory.newInstance().newTransformer();
                transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
                transformer.transform(new DOMSource(styles.item(i)), new StreamResult(writer));
                String output = writer.toString().replace(" xmlns=\"http://www.w3.org/2000/svg\"","");

                //System.out.println("style is "+output);
                String styleEscaped = output.replace("<", "%3C");
                styleEscaped = styleEscaped.replace(">", "%3E");
                styleEscaped = styleEscaped.replace("\"", "'");
                styleEscaped = styleEscaped.replace("#", "%23");
                styleEscaped = styleEscaped.replace("\n", "");
                styleEscaped = styleEscaped.replace("\r", "");
                sb.append(styleEscaped);
                //System.out.println("sb is now "+ sb);
            }

            NodeList polygons = doc.getElementsByTagName("polygon");
            for(int i=0; i < polygons.getLength(); i++) {
                StringWriter writer = new StringWriter();
                transformer = TransformerFactory.newInstance().newTransformer();
                transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
                transformer.transform(new DOMSource(polygons.item(i)), new StreamResult(writer));
                String output = writer.toString().replace(" xmlns=\"http://www.w3.org/2000/svg\"","");

                String polygonEscaped = output.replace("<", "%3C");
                polygonEscaped = polygonEscaped.replace(">", "%3E");
                polygonEscaped = polygonEscaped.replace("\"", "'");
                polygonEscaped = polygonEscaped.replace("#", "%23");
                sb.append(polygonEscaped);
            }

            NodeList circles = doc.getElementsByTagName("circle");
            for(int i=0; i < circles.getLength(); i++) {
                StringWriter writer = new StringWriter();
                transformer = TransformerFactory.newInstance().newTransformer();
                transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
                transformer.transform(new DOMSource(circles.item(i)), new StreamResult(writer));
                String output = writer.toString().replace(" xmlns=\"http://www.w3.org/2000/svg\"","");

                String circleEscaped = output.replace("<", "%3C");
                circleEscaped = circleEscaped.replace(">", "%3E");
                circleEscaped = circleEscaped.replace("\"", "'");
                circleEscaped = circleEscaped.replace("#", "%23");
                System.out.println("Found circle"+circleEscaped);
                sb.append(circleEscaped);
            }

            NodeList paths = doc.getElementsByTagName("path");
;           for(int i=0; i < paths.getLength(); i++) {
                //String path = paths.item(i).toString();

                StringWriter writer = new StringWriter();
                transformer = TransformerFactory.newInstance().newTransformer();
                transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
                transformer.transform(new DOMSource(paths.item(i)), new StreamResult(writer));
                String output = writer.toString().replace(" xmlns=\"http://www.w3.org/2000/svg\"","");

                //System.out.println("path is "+output);
                String pathEscaped = output.replace("<", "%3C");
                pathEscaped = pathEscaped.replace(">", "%3E");
                pathEscaped = pathEscaped.replace("\"", "'");
                pathEscaped = pathEscaped.replace("#", "%23");
                sb.append(pathEscaped);
                //pathEscaped = "data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='24' height='24' viewBox='0 0 24 24' "+ pathEscaped + "%3C/svg%3E";
                //System.out.println("sb is now "+ sb);
            }

            NodeList rects = doc.getElementsByTagName("rect");
            for(int i=0; i < rects.getLength(); i++) {
                System.out.println("Found rect");
                StringWriter writer = new StringWriter();
                transformer = TransformerFactory.newInstance().newTransformer();
                transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
                transformer.transform(new DOMSource(rects.item(i)), new StreamResult(writer));
                String output = writer.toString().replace(" xmlns=\"http://www.w3.org/2000/svg\"","");

                String rectEscaped = output.replace("<", "%3C");
                rectEscaped = rectEscaped.replace(">", "%3E");
                rectEscaped = rectEscaped.replace("\"", "'");
                rectEscaped = rectEscaped.replace("#", "%23");
                System.out.println("Found rect"+rectEscaped);
                sb.append(rectEscaped);
            }
            sb.append("%3C/svg%3E");

            File file = new File(elementTemplateLocation);
            String content = FileUtils.readFileToString(file, "utf-8");

            // Get array of element templates and see if it exists to overwrite
            JSONArray jsonArray = new JSONArray(content);
            //System.out.println("JSON is "+jsonArray.toString());
            ListIterator iter = jsonArray.toList().listIterator();
            while(iter.hasNext()){
                int index = iter.nextIndex();
                HashMap json = (HashMap) iter.next();
                String id = (String) json.get("id");
                //System.out.println("id is "+id);
                if (templateId.equals(id)) {
                    templateMatch = true;
                    System.out.println("Matching ID found");
                    //Overwrite existing entry
                    jsonArray.remove(index);

                    jsonArray = addOrUpdateArray(jsonArray,index, templateName, templateId, appliesTo, sb, templateMatch);

                }

            }

            if (!templateMatch){
                //Add entry in element templates
                jsonArray = addOrUpdateArray(jsonArray,0 , templateName, templateId, appliesTo, sb, templateMatch);
            }

            //Write to file
            try (PrintWriter out = new PrintWriter(new FileWriter(elementTemplateLocation))) {
                out.write(jsonArray.toString());
            } catch (Exception e) {
                e.printStackTrace();
            }

            System.out.println("final sb is "+sb);

        } catch (Exception e) {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            e.printStackTrace(pw);
            String sStackTrace = sw.toString(); // stack trace as a string
            System.out.println("Uh oh "+sStackTrace);
        }
    }

    private static JSONArray addOrUpdateArray(JSONArray jsonArray, int index, String templateName, String templateId, String appliesTo, StringBuilder sb, boolean templateMatch) {

        //Create new Object
        JSONObject updatedEntry = new JSONObject();
        updatedEntry.put("$schema", "https://unpkg.com/@camunda/zeebe-element-templates-json-schema/resources/schema.json");
        updatedEntry.put("name", templateName);
        updatedEntry.put("id", templateId);
        updatedEntry.put("description", templateName);
        updatedEntry.put("documentationRef", "https://docs.camunda.io");

        JSONArray appliesToArray = new JSONArray();
        appliesToArray.put(appliesTo);
        updatedEntry.put("appliesTo", appliesToArray);

        JSONObject value = new JSONObject();
        value.put("value", appliesTo);
        updatedEntry.put("elementType", value);

        JSONObject contents = new JSONObject();
        contents.put("contents", sb.toString());
        updatedEntry.put("icon", contents);

        JSONObject propertiesValues = new JSONObject();
        propertiesValues.put("type", "String");
        propertiesValues.put("value", templateName);

        JSONObject bindingsValues = new JSONObject();
        bindingsValues.put("type", "property");
        bindingsValues.put("name", "name");

        propertiesValues.put("binding", bindingsValues);

        JSONArray properties = new JSONArray();
        properties.put(propertiesValues);

        updatedEntry.put("properties", properties);

        //Put back or add into original array
        if(templateMatch) {
            jsonArray.put(index, updatedEntry);
        } else {
            jsonArray.put(updatedEntry);
        }

        return jsonArray;
    }
}
