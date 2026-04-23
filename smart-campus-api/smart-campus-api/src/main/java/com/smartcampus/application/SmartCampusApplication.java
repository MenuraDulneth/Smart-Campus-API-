package com.smartcampus.application;

import javax.ws.rs.ApplicationPath;
import javax.ws.rs.core.Application;
import java.util.HashSet;
import java.util.Set;

import com.smartcampus.resource.RoomResource;
import com.smartcampus.resource.SensorResource;
import com.smartcampus.resource.DiscoveryResource;
import com.smartcampus.exception.*;
import com.smartcampus.filter.LoggingFilter;

/**
 * JAX-RS Application configuration class.
 *
 * The @ApplicationPath annotation establishes the versioned base URI for all API endpoints.
 * All registered resources and providers are loaded here.
 *
 * Lifecycle Note:
 * By default, JAX-RS resource classes are request-scoped — a new instance is created for
 * every incoming HTTP request. This class explicitly registers all components so the
 * runtime has full control.
 */
@ApplicationPath("/api/v1")
public class SmartCampusApplication extends Application {

    @Override
    public Set<Class<?>> getClasses() {
        Set<Class<?>> classes = new HashSet<>();

        // Resource classes
        classes.add(DiscoveryResource.class);
        classes.add(RoomResource.class);
        classes.add(SensorResource.class);

        // Exception Mappers
        classes.add(RoomNotEmptyExceptionMapper.class);
        classes.add(LinkedResourceNotFoundExceptionMapper.class);
        classes.add(SensorUnavailableExceptionMapper.class);
        classes.add(GlobalExceptionMapper.class);

        // Filters
        classes.add(LoggingFilter.class);

        return classes;
    }
}
