package com.khalil.supply.ride;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface RideRepository extends JpaRepository<Ride, Long> {

    /**
     * Fetches rides together with line, partner and both brands in a single query,
     * avoiding the N+1 you'd get from lazily walking each association per ride.
     * Brand filter matches the <em>effective</em> brand (ride-level, else line-level).
     */
    @Query("""
            select r from Ride r
              join fetch r.line l
              join fetch r.partner
              left join fetch r.brand rb
              left join fetch l.brand lb
            where (:lineCode is null or l.code = :lineCode)
              and (:brandCode is null or coalesce(rb.code, lb.code) = :brandCode)
            order by r.departureAt
            """)
    List<Ride> search(@Param("lineCode") String lineCode, @Param("brandCode") String brandCode);

    @Query("""
            select distinct r from Ride r
              join fetch r.line l
              join fetch r.partner
              left join fetch r.brand
              left join fetch l.brand
              left join fetch r.stops
            where r.id = :id
            """)
    Optional<Ride> findDetailedById(@Param("id") Long id);
}
