package no.nav.syfo.util;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static java.util.Optional.of;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Stream.empty;

public class MapUtil {
    private MapUtil() {
    }

    public static <T, R, S extends R> S mapNullable(T fra, S til, BiConsumer<T, R> exp) {
        return ofNullable(fra).map(f -> {exp.accept(f, til); return til;}).orElse(null);
    }

    public static <T, R, S extends R> S mapNullable(T fra, S til, BiConsumer<T, R> exp, S other) {
        return ofNullable(fra).map(f -> {exp.accept(f, til); return til;}).orElse(other);
    }

    public static <T, R, S extends R> S map(T fra, S til, BiConsumer<T, R> exp) {
        return of(fra).map(f -> {exp.accept(f, til); return til;}).orElseThrow(() -> new RuntimeException("Resultatet fra exp ble null"));
    }

    public static <T, U extends T, R, S extends R> List<S> mapListe(List<U> fra, Supplier<S> til, BiConsumer<T, R> exp) {
        return ofNullable(fra).map(f -> mapStream(f.stream(), til, exp).collect(toList())).orElse(new ArrayList<>());
    }

    public static <T, U extends T, R, S extends R> List<S> mapListe(List<U> fra, Supplier<S> til, Predicate<U> filter, BiConsumer<T, R> exp) {
        return ofNullable(fra).map(f -> mapStream(f.stream().filter(filter), til, exp).collect(toList())).orElse(new ArrayList<>());
    }

    public static <T, U extends T, R, S extends R> Stream<S> mapStream(Stream<U> fra, Supplier<S> til, BiConsumer<T, R> exp) {
        return ofNullable(fra).map(f -> f.map(f1 -> {S s = til.get(); exp.accept(f1, s); return s;})).orElse(empty());
    }

    public static <T, U extends T, R, S extends R> List<S> mapListe(List<U> fra, Function<U, S> til, BiConsumer<T, R> exp) {
        return ofNullable(fra).map(f -> mapStream(f.stream(), til, exp).collect(toList())).orElse(new ArrayList<>());
    }

    public static <T, U extends T, R, S extends R> List<S> mapListe(List<U> fra, Function<U, S> til, Predicate<U> filter, BiConsumer<T, R> exp) {
        return ofNullable(fra).map(f -> mapStream(f.stream().filter(filter), til, exp).collect(toList())).orElse(new ArrayList<>());
    }

    public static <T, U extends T, R, S extends R> Stream<S> mapStream(Stream<U> fra, Function<U, S> til, BiConsumer<T, R> exp) {
        return ofNullable(fra).map(f -> f.map(f1 -> {S s = map(f1, til); exp.accept(f1, s); return s;})).orElse(empty());
    }

    public static <T, R> R mapNullable(T fra, Function<T, R> exp) {
        return ofNullable(fra).map(exp).orElse(null);
    }

    public static <T, R> R mapNullable(T fra, Function<T, R> exp, R other) {
        return ofNullable(fra).map(exp).orElse(other);
    }

    public static <T, R> R mapNullable(T fra, Predicate<T> filter, Function<T, R> exp) {
        return ofNullable(fra).filter(filter).map(exp).orElse(null);
    }

    public static <T> T mapNullable(T fra, T other) {
        return ofNullable(fra).orElse(other);
    }

    public static <T, R> R map(T fra, Function<T, R> exp) {
        return of(fra).map(exp).orElseThrow(() -> new RuntimeException("Resultatet fra exp ble null"));
    }

    public static <T, R> R mapMangetilEn(List<T> fra, Predicate<T> selector, Function<T, R> exp) {
        return ofNullable(fra).flatMap(g -> g.stream().filter(selector).map(exp).findFirst()).orElse(null);
    }

    public static <T, R> List<R> mapListe(List<T> fra, Predicate<T> filter, Function<T, R> exp) {
        return ofNullable(fra).map(f -> mapStream(f.stream().filter(filter), exp).collect(toList())).orElse(new ArrayList<>());
    }

    public static <T, R> List<R> mapListe(List<T> fra, Function<T, R> exp) {
        return ofNullable(fra).map(f -> mapStream(f.stream(), exp).collect(toList())).orElse(new ArrayList<>());
    }

    public static <T, R> Stream<R> mapStream(Stream<T> fra, Function<T, R> exp) {
        return ofNullable(fra).map(f -> f.map(exp)).orElse(empty());
    }
}
