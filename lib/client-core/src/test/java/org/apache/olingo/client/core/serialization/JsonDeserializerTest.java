package org.apache.olingo.client.core.serialization;

import org.apache.olingo.client.api.data.ResWrap;
import org.apache.olingo.commons.api.Constants;
import org.apache.olingo.commons.api.data.Entity;
import org.apache.olingo.commons.api.data.Property;
import org.apache.olingo.commons.api.data.ValueType;
import org.apache.olingo.commons.api.edm.geo.Geospatial;
import org.apache.olingo.commons.api.edm.geo.GeospatialCollection;
import org.apache.olingo.commons.api.edm.geo.Point;
import org.apache.olingo.commons.api.edm.geo.SRID;
import org.apache.olingo.commons.api.format.ContentType;
import org.junit.Assert;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.OutputStreamWriter;
import java.util.Collections;

public class JsonDeserializerTest {

  private static final JsonSerializer SERIALIZER = new JsonSerializer(false, ContentType.APPLICATION_JSON);
  private static final JsonDeserializer DESERIALIZER = new JsonDeserializer(false);

  private static final Point POINT = new Point(Geospatial.Dimension.GEOMETRY, SRID.valueOf("4326"));

  static {
    POINT.setX(1.0);
    POINT.setY(1.0);
    POINT.setZ(1.0);
  }

  @Test
  public void toEntityWithPointField() throws Exception {

    ByteArrayOutputStream output = new ByteArrayOutputStream();
    OutputStreamWriter writer = new OutputStreamWriter(output, Constants.UTF8);

    Entity entity = new Entity();
    final Property propertyResource = new Property();
    propertyResource.setName("GeometryPointField");
    propertyResource.setValue(ValueType.GEOSPATIAL, POINT);
    entity.getProperties().add(propertyResource);

    SERIALIZER.write(writer, new ResWrap(null, null, entity));

    ResWrap<Entity> entityResWrap = DESERIALIZER.toEntity(new ByteArrayInputStream(output.toByteArray()));

    Property geometryPoint = entityResWrap.getPayload().getProperty("GeometryPointField");
    Assert.assertNotNull(geometryPoint);
    ValueType actualValueType = geometryPoint.getValueType();
    Assert.assertEquals("Unexpected Point type", ValueType.GEOSPATIAL, actualValueType);
    Assert.assertTrue(geometryPoint.asGeospatial() instanceof Point);
  }

  @Test
  public void toEntityWithGeometryCollectionField() throws Exception {

    GeospatialCollection collection = new GeospatialCollection(Geospatial.Dimension.GEOMETRY, SRID.valueOf("4326"),
                                                               Collections.<Geospatial>singletonList(POINT));
    ByteArrayOutputStream output = new ByteArrayOutputStream();
    OutputStreamWriter writer = new OutputStreamWriter(output, Constants.UTF8);

    Entity entity = new Entity();
    final Property propertyResource = new Property();
    propertyResource.setName("GeometryCollectionField");
    propertyResource.setValue(ValueType.GEOSPATIAL, collection);
    entity.getProperties().add(propertyResource);

    // serilizes geometry collection as:
    // {
    // "GeometryCollectionField":{
    // "type":"GeometryCollection",
    // "geometries":[
    //   {
    //     "type":"Point",
    //     "coordinates":[ 1.0, 1.0 ],
    //     "crs":{
    //       "type":"name",
    //       "properties":{
    //          "name":"EPSG:4326"
    //        }
    //      }
    //    }
    // ],
    // "crs":{
    //   "type":"name",
    //   "properties":{
    //     "name":"EPSG:4326"
    //    }
    //   }
    //  }
    // }

    SERIALIZER.write(writer, new ResWrap(null, null, entity));
    System.out.println(new String(output.toByteArray()));

    ResWrap<Entity> entityResWrap = DESERIALIZER.toEntity(new ByteArrayInputStream(output.toByteArray()));
    Property geometryCollection = entityResWrap.getPayload().getProperty("GeometryCollectionField");
    Assert.assertNotNull(geometryCollection);

    ValueType actualValueType = geometryCollection.getValueType();
    Assert.assertEquals("Unexpected GeospatialCollection type", ValueType.GEOSPATIAL, actualValueType);
    Assert.assertTrue(geometryCollection.asGeospatial() instanceof GeospatialCollection);
  }
}
