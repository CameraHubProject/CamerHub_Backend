-- Preserve historical inspection facts in the product response without restoring the retired role.
UPDATE camerahub.products product
SET inspection_checks = history.checks
FROM (
    SELECT request.product_id,
           jsonb_agg(jsonb_build_object('name',definition.display_name,'result',result.result,'notes',result.notes)
                     ORDER BY definition.code) AS checks
    FROM camerahub.inspection_requests request
    JOIN camerahub.inspection_reports report ON report.request_id=request.id
    JOIN camerahub.inspection_results result ON result.report_id=report.id
    JOIN camerahub.inspection_check_definitions definition ON definition.code=result.check_code
    WHERE report.superseded_at IS NULL
      AND report.id=(SELECT latest.id FROM camerahub.inspection_reports latest
                     JOIN camerahub.inspection_requests q ON q.id=latest.request_id
                     WHERE q.product_id=request.product_id AND latest.superseded_at IS NULL
                     ORDER BY latest.issued_at DESC LIMIT 1)
    GROUP BY request.product_id
) history
WHERE product.id=history.product_id AND product.inspection_checks='[]'::jsonb;
