package iboot.authz

# 与 Java 本地策略一致的 first-match 语义：
# - 规则按 extra-rules、rules 的顺序由调用方传入；
# - 首个命中路径的规则决定允许或拒绝；
# - 未命中任何规则时放行，由 JWT/白名单继续处理。
default allow := true

allow := false if {
    some i, rule in input.rules
    some pattern in rule.paths
    path_matches(pattern, input.path)
    not caller_allowed(rule, input.caller)
    not earlier_match(i, input)
}

path_matches(pattern, path) if {
    glob.match(pattern, ["/"], path)
}

# AntPathMatcher 的 /foo/** 也匹配 /foo 本身；OPA 的 glob 需要补这一条。
path_matches(pattern, path) if {
    endswith(pattern, "/**")
    path == trim_suffix(pattern, "/**")
}

caller_allowed(rule, caller) if {
    some allowed in rule.callers
    allowed == caller
}

earlier_match(i, inp) if {
    some j, other in inp.rules
    j < i
    some pattern in other.paths
    path_matches(pattern, input.path)
}
