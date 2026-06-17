package com.stie.service;

import com.stie.model.Location;
import com.stie.model.Tenant;
import com.stie.repository.LocationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class LocationService {

    @Autowired
    private LocationRepository locationRepository;

    public List<Location> getLocationsByTenant(Tenant tenant) {
        return locationRepository.findByTenantOrderByNameAsc(tenant);
    }

    public Optional<Location> getById(Long id) {
        return locationRepository.findById(id);
    }

    public Location create(String name, Tenant tenant) {
        Location loc = new Location(name, tenant);
        return locationRepository.save(loc);
    }

    public Location update(Long id, String name) {
        Location loc = locationRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Location not found: " + id));
        loc.setName(name);
        return locationRepository.save(loc);
    }

    public void delete(Long id) {
        locationRepository.deleteById(id);
    }
}

