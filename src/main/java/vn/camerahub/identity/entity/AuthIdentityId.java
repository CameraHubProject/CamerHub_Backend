package vn.camerahub.identity.entity;

import java.io.Serializable;
import java.util.UUID;
import java.time.*;

@lombok.Data
@lombok.NoArgsConstructor
@lombok.AllArgsConstructor
public class AuthIdentityId implements Serializable {
    private static final long serialVersionUID = 1L;
    private String provider;
    private String subject;
}
