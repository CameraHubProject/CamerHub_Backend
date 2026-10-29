ALTER TABLE camerahub.camera_models
    DROP CONSTRAINT camera_models_device_type_check;
ALTER TABLE camerahub.camera_models
    ADD CONSTRAINT camera_models_device_type_check
    CHECK (device_type IN ('BODY', 'BODY_LENS', 'LENS', 'OTHER'));
