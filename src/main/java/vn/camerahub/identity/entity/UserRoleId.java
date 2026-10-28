package vn.camerahub.identity.entity;

import java.io.Serializable;
import java.util.UUID;
import java.time.*;

@lombok.Data
@lombok.NoArgsConstructor
@lombok.AllArgsConstructor
public class UserRoleId implements Serializable {
    private static final long serialVersionUID = 1L;
    private UUID userId;
    private String roleCode;
}
