(add-hook
  'nxml-mode-hook
  (lambda ()
    (setq rng-schema-locating-files-default
          (append '("/sandbox/docbook/trunk/xsl/locatingrules.xml")
                  rng-schema-locating-files-default ))))
