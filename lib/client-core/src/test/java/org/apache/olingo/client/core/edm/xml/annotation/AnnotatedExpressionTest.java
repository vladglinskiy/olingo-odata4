package org.apache.olingo.client.core.edm.xml.annotation;

import org.apache.olingo.client.api.edm.xml.XMLMetadata;
import org.apache.olingo.client.core.serialization.ClientODataDeserializerImpl;
import org.apache.olingo.commons.api.edm.provider.CsdlAnnotation;
import org.apache.olingo.commons.api.edm.provider.annotation.CsdlExpression;
import org.apache.olingo.commons.api.edm.provider.annotation.CsdlLogicalOrComparisonExpression;
import org.apache.olingo.commons.api.edm.provider.annotation.CsdlNull;
import org.apache.olingo.commons.api.edm.provider.annotation.CsdlRecord;
import org.apache.olingo.commons.api.edm.provider.annotation.CsdlUrlRef;
import org.apache.olingo.commons.api.format.ContentType;
import org.junit.Assert;
import org.junit.Test;

import java.io.ByteArrayInputStream;

public class AnnotatedExpressionTest {

  /**
   * Record's annotation is parsed as expected. Test is added to demonstrate expected behaviour.
   */
  @Test
  public void annotatedRecordExpressionTest() throws Exception {
    ClientODataDeserializerImpl deserializer = new ClientODataDeserializerImpl(false, ContentType.APPLICATION_XML);
    String annotation =
      "<Record>\n" +
      "  <Annotation Term=\"Core.Description\" String=\"Annotation on record\" />\n" +
      "  <PropertyValue Property=\"GivenName\" Path=\"String\"/>\n" +
      "  <PropertyValue Property=\"Age\" Path=\"Byte\"/>\n" +
      "</Record>";
    String serviceDocument = serviceDocumentWithAnnotationExpression(annotation);
    XMLMetadata metadata = deserializer.toMetadata(new ByteArrayInputStream(serviceDocument.getBytes()));

    CsdlExpression expression = getParsedExpression(metadata);
    Assert.assertNotNull(expression);

    CsdlRecord record = expression.asDynamic().asRecord();
    Assert.assertNotNull(record);

    Assert.assertNotNull(record.getAnnotations());
    Assert.assertEquals(1, record.getAnnotations().size());

    CsdlAnnotation recordAnnotation = record.getAnnotations().get(0);
    Assert.assertEquals("Core.Description", recordAnnotation.getTerm());
    Assert.assertNotNull(recordAnnotation.getExpression());
    Assert.assertEquals("Annotation on record", recordAnnotation.getExpression().asConstant().getValue());
  }

  /**
   * Annotations on logical expressions are not parsed at all.
   */
  @Test
  public void annotatedAndExpressionTest() throws Exception {
    ClientODataDeserializerImpl deserializer = new ClientODataDeserializerImpl(false, ContentType.APPLICATION_XML);
    String annotation =
      "<And>\n" +
      "  <Annotation Term=\"Core.Description\" String=\"Annotation on and\" />\n" +
      "  <Path>Boolean</Path>\n" +
      "  <Path>Boolean</Path>\n" +
      "</And>";
    String serviceDocument = serviceDocumentWithAnnotationExpression(annotation);
    XMLMetadata metadata = deserializer.toMetadata(new ByteArrayInputStream(serviceDocument.getBytes()));

    CsdlExpression expression = getParsedExpression(metadata);
    Assert.assertNotNull(expression);

    CsdlLogicalOrComparisonExpression and = expression.asDynamic().asLogicalOrComparison();
    Assert.assertNotNull(and);

    Assert.assertNotNull(and.getAnnotations());
    // annotations on logical expressions are not parsed, the following assertion fails
    Assert.assertEquals(1, and.getAnnotations().size());

    CsdlAnnotation recordAnnotation = and.getAnnotations().get(0);
    Assert.assertEquals("Core.Description", recordAnnotation.getTerm());
    Assert.assertNotNull(recordAnnotation.getExpression());
    Assert.assertEquals("Annotation on and", recordAnnotation.getExpression().asConstant().getValue());
  }

  /**
   * Annotations on Url Ref expressions are not parsed at all.
   */
  @Test
  public void annotatedUrlRefExpressionTest() throws Exception {
    ClientODataDeserializerImpl deserializer = new ClientODataDeserializerImpl(false, ContentType.APPLICATION_XML);
    String annotation =
      "<UrlRef>\n" +
        "  <String>http://host/wiki/HowToUse</String>\n" +
        "  <Annotation Term=\"Core.Description\" String=\"Annotation on url ref\" />\n" +
        "</UrlRef>";
    String serviceDocument = serviceDocumentWithAnnotationExpression(annotation);
    XMLMetadata metadata = deserializer.toMetadata(new ByteArrayInputStream(serviceDocument.getBytes()));

    CsdlExpression expression = getParsedExpression(metadata);
    Assert.assertNotNull(expression);

    CsdlUrlRef urlRef = expression.asDynamic().asUrlRef();
    Assert.assertNotNull(urlRef);

    Assert.assertNotNull(urlRef.getAnnotations());
    // annotations on url ref expressions are not parsed, the following assertion fails
    Assert.assertEquals(1, urlRef.getAnnotations().size());

    CsdlAnnotation recordAnnotation = urlRef.getAnnotations().get(0);
    Assert.assertEquals("Core.Description", recordAnnotation.getTerm());
    Assert.assertNotNull(recordAnnotation.getExpression());
    Assert.assertEquals("Annotation on url ref", recordAnnotation.getExpression().asConstant().getValue());
  }

  /**
   * Annotations on the following expressions get wrapped into another CsdlAnnotation instance due to missing
   * 'jp.nextToken()' calls in the corresponding deserializers:
   * - ClientCsdlNull
   * - ClientCsdlApply
   * - ClientCsdlCast
   * - ClientCsdlIsOf
   * - ClientCsdlLabeledElement
   */
  @Test
  public void annotatedNullExpressionTest() throws Exception {
    ClientODataDeserializerImpl deserializer = new ClientODataDeserializerImpl(false, ContentType.APPLICATION_XML);
    String annotation = "<Null><Annotation Term=\"Core.Description\" String=\"Annotation on null\" /></Null>";
    String serviceDocument = serviceDocumentWithAnnotationExpression(annotation);
    XMLMetadata metadata = deserializer.toMetadata(new ByteArrayInputStream(serviceDocument.getBytes()));

    CsdlExpression expression = getParsedExpression(metadata);
    Assert.assertNotNull(expression);

    CsdlNull csdlNull = expression.asDynamic().asNull();
    Assert.assertNotNull(csdlNull);

    Assert.assertNotNull(csdlNull.getAnnotations());
    Assert.assertEquals(1, csdlNull.getAnnotations().size());

    CsdlAnnotation recordAnnotation = csdlNull.getAnnotations().get(0);
    // due to missing 'jp.nextToken()' call at ClientCsdlNull:46 annotation gets wrapped into another CsdlAnnotation
    Assert.assertTrue(recordAnnotation.getAnnotations() == null || recordAnnotation.getAnnotations().isEmpty());
    Assert.assertEquals("Core.Description", recordAnnotation.getTerm());
    Assert.assertNotNull(recordAnnotation.getExpression());
    Assert.assertEquals("Annotation on null", recordAnnotation.getExpression().asConstant().getValue());
  }

  private String serviceDocumentWithAnnotationExpression(String annotationExpression) {
    return "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
      "<edmx:Edmx xmlns:edmx=\"http://docs.oasis-open.org/odata/ns/edmx\" Version=\"4.0\">\n" +
      "  <edmx:DataServices>\n" +
      "    <Schema xmlns=\"http://docs.oasis-open.org/odata/ns/edm\" Namespace=\"Annotations\">\n" +
      "      <Annotations Target=\"Test\">\n" +
      "        <Annotation Term=\"Core.Description\">\n" +
      "\n" +
      annotationExpression +
      "\n" +
      "        </Annotation>\n" +
      "      </Annotations>\n" +
      "    </Schema>\n" +
      "  </edmx:DataServices>\n" +
      "</edmx:Edmx>";
  }

  private CsdlExpression getParsedExpression(XMLMetadata metadata) {
    return metadata.getSchemas().get(0).getAnnotationGroups().get(0).getAnnotations().get(0).getExpression();
  }
}