package com.aurora.common.constant;

/**
 * 权限码常量
 *
 * <p>spec: docs/specs/2026-07-05-global-variables.md §4.2.4</p>
 * <p>命名规则：模块:资源:动作，三段式，全小写，kebab-case。</p>
 * <p>用法示例：{@code @SaCheckPermission(PermCodeConst.System.User.ADD)}</p>
 *
 * <p>类层级：模块 → 资源 → 动作字符串常量。每个内部类私有构造，防止实例化。</p>
 */
public final class PermCodeConst {

    private PermCodeConst() {
    }

    /** 系统模块（基座 RBAC） */
    public static final class System {
        private System() {}

        public static final class User {
            private User() {}
            public static final String LIST           = "system:user:list";
            public static final String ADD            = "system:user:add";
            public static final String EDIT           = "system:user:edit";
            public static final String REMOVE         = "system:user:remove";
            public static final String RESET_PASSWORD = "system:user:reset-password";
            public static final String STATUS         = "system:user:status";
        }

        public static final class Role {
            private Role() {}
            public static final String LIST        = "system:role:list";
            public static final String ADD         = "system:role:add";
            public static final String EDIT        = "system:role:edit";
            public static final String REMOVE      = "system:role:remove";
            public static final String ASSIGN_MENU = "system:role:assign-menu";
        }

        public static final class Menu {
            private Menu() {}
            public static final String LIST   = "system:menu:list";
            public static final String ADD    = "system:menu:add";
            public static final String EDIT   = "system:menu:edit";
            public static final String REMOVE = "system:menu:remove";
        }

        public static final class Log {
            private Log() {}
            public static final String LIST   = "system:log:list";
            public static final String REMOVE = "system:log:remove";
        }

        public static final class File {
            private File() {}
            public static final String UPLOAD = "system:file:upload";
        }
    }

    /** HR 模块（Phase 2） */
    public static final class Hr {
        private Hr() {}

        public static final class Dept {
            private Dept() {}
            public static final String LIST   = "hr:dept:list";
            public static final String ADD    = "hr:dept:add";
            public static final String EDIT   = "hr:dept:edit";
            public static final String REMOVE = "hr:dept:remove";
        }

        public static final class Position {
            private Position() {}
            public static final String LIST   = "hr:position:list";
            public static final String ADD    = "hr:position:add";
            public static final String EDIT   = "hr:position:edit";
            public static final String REMOVE = "hr:position:remove";
        }

        public static final class Employee {
            private Employee() {}
            public static final String LIST     = "hr:employee:list";
            public static final String ADD      = "hr:employee:add";
            public static final String EDIT     = "hr:employee:edit";
            public static final String REMOVE   = "hr:employee:remove";
            public static final String EXPORT   = "hr:employee:export";
            public static final String TRANSFER = "hr:employee:transfer";
        }

        public static final class Attendance {
            private Attendance() {}
            public static final String CLOCK_IN = "hr:attendance:clock-in";
            public static final String AUDIT    = "hr:attendance:audit";
        }
    }

    /** EDU 模块（Phase 3） */
    public static final class Edu {
        private Edu() {}

        public static final class Student {
            private Student() {}
            public static final String LIST   = "edu:student:list";
            public static final String ADD    = "edu:student:add";
            public static final String EDIT   = "edu:student:edit";
            public static final String REMOVE = "edu:student:remove";
        }

        public static final class Teacher {
            private Teacher() {}
            public static final String LIST   = "edu:teacher:list";
            public static final String ADD    = "edu:teacher:add";
            public static final String EDIT   = "edu:teacher:edit";
            public static final String REMOVE = "edu:teacher:remove";
        }

        public static final class Course {
            private Course() {}
            public static final String LIST   = "edu:course:list";
            public static final String ADD    = "edu:course:add";
            public static final String EDIT   = "edu:course:edit";
            public static final String REMOVE = "edu:course:remove";
        }

        public static final String SCORE_INPUT        = "edu:score:input";
        public static final String SCORE_VIEW_MY      = "edu:score:view-my";
        public static final String SELECTION_SELECT   = "edu:selection:select";
        public static final String SELECTION_DROP     = "edu:selection:drop";
    }

    /** OJ 模块（Phase 4） */
    public static final class Oj {
        private Oj() {}

        public static final class Problem {
            private Problem() {}
            public static final String LIST   = "oj:problem:list";
            public static final String DETAIL = "oj:problem:detail";
            public static final String ADD    = "oj:problem:add";
            public static final String EDIT   = "oj:problem:edit";
            public static final String REMOVE = "oj:problem:remove";
        }

        public static final String SUBMISSION_SUBMIT  = "oj:submission:submit";
        public static final String SUBMISSION_VIEW_MY = "oj:submission:view-my";
        public static final String FAVORITE_VIEW_MY   = "oj:favorite:view-my";
        public static final String LEADERBOARD_VIEW   = "oj:leaderboard:view";
    }

    /** MALL 模块（Phase 5，答辩核心） */
    public static final class Mall {
        private Mall() {}

        public static final class Product {
            private Product() {}
            public static final String LIST   = "mall:product:list";
            public static final String ADD    = "mall:product:add";
            public static final String EDIT   = "mall:product:edit";
            public static final String REMOVE = "mall:product:remove";
            public static final String STATUS = "mall:product:status";
        }

        public static final class Cart {
            private Cart() {}
            public static final String LIST   = "mall:cart:list";
            public static final String ADD    = "mall:cart:add";
            public static final String EDIT   = "mall:cart:edit";
            public static final String REMOVE = "mall:cart:remove";
        }

        public static final class Address {
            private Address() {}
            public static final String LIST   = "mall:address:list";
            public static final String ADD    = "mall:address:add";
            public static final String EDIT   = "mall:address:edit";
            public static final String REMOVE = "mall:address:remove";
        }

        public static final class Order {
            private Order() {}
            public static final String PLACE   = "mall:order:place";
            public static final String VIEW_MY = "mall:order:view-my";
            public static final String LIST    = "mall:order:list";
            public static final String PRICE   = "mall:order:price";
            public static final String SHIP    = "mall:order:ship";
            public static final String REFUND  = "mall:order:refund";
        }

        public static final String SECKILL_JOIN = "mall:seckill:join";
    }

    /** AI 模块（Phase 6） */
    public static final class Ai {
        private Ai() {}

        public static final class Document {
            private Document() {}
            public static final String UPLOAD = "ai:document:upload";
            public static final String REMOVE = "ai:document:remove";
        }

        public static final String CHAT_SEND    = "ai:chat:send";
        public static final String CHAT_HISTORY = "ai:chat:history";
    }
}
