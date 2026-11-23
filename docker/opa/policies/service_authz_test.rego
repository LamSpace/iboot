package iboot.authz

test_allow_gateway_api if {
    allow with input as {
        "caller": "iboot-gateway",
        "path": "/api/user/1",
        "rules": [{"paths": ["/api/**"], "callers": ["iboot-gateway"]}]
    }
}

test_deny_file_api if {
    not allow with input as {
        "caller": "iboot-file",
        "path": "/api/user/1",
        "rules": [{"paths": ["/api/**"], "callers": ["iboot-gateway"]}]
    }
}

test_unmatched_path_allowed if {
    allow with input as {
        "caller": "iboot-file",
        "path": "/actuator/health",
        "rules": [{"paths": ["/api/**"], "callers": ["iboot-gateway"]}]
    }
}

test_first_match_denies_even_if_later_allows if {
    not allow with input as {
        "caller": "iboot-file",
        "path": "/api/monitor/backup/1",
        "rules": [
            {"paths": ["/api/monitor/backup/**"], "callers": ["iboot-ai"]},
            {"paths": ["/api/**"], "callers": ["iboot-file"]}
        ]
    }
}

test_first_match_allows_even_if_later_denies if {
    allow with input as {
        "caller": "iboot-ai",
        "path": "/api/monitor/backup/1",
        "rules": [
            {"paths": ["/api/monitor/backup/**"], "callers": ["iboot-ai"]},
            {"paths": ["/api/**"], "callers": ["iboot-gateway"]}
        ]
    }
}

test_empty_caller_denied if {
    not allow with input as {
        "caller": "",
        "path": "/api/user/1",
        "rules": [{"paths": ["/api/**"], "callers": ["iboot-gateway"]}]
    }
}

test_bare_internal_wildcard_denied if {
    not allow with input as {
        "caller": "iboot-file",
        "path": "/internal/leave/quota",
        "rules": [{"paths": ["/internal/leave/quota/**"], "callers": ["iboot-workflow"]}]
    }
}

test_bare_api_wildcard_allowed if {
    not allow with input as {
        "caller": "iboot-file",
        "path": "/api",
        "rules": [{"paths": ["/api/**"], "callers": ["iboot-gateway"]}]
    }
}
