package com.yeswater.iam.domain.support;

/**
 * 所属应用编码解析。
 */
public final class AppCodeResolver {

    private AppCodeResolver() {
    }

    /**
     * 根据显式应用编码或资源标识推断所属应用。
     */
    public static String resolve(String explicitAppCode, String resourceCode) {
        if (explicitAppCode != null && !explicitAppCode.isBlank()) {
            return explicitAppCode.trim().toUpperCase();
        }
        if (resourceCode != null && resourceCode.contains(":")) {
            return resourceCode.substring(0, resourceCode.indexOf(':')).trim().toUpperCase();
        }
        return "IAM";
    }
}
