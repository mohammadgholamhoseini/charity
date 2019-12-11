@extends('admin.layout.master')
@section('content')

    <section class="content-header">
        @include('admin.layout.form-errors')
        <h1>
            ایجاد دسته بندی جدید
        </h1>
        <div class="col-md-10 offset-md-1">
            <form method="POST" action="{{route('categories.store')}}">
                @csrf
                <div class="form-group">
                    <label>عنوان</label>
                    <input type="text" class="form-control r" name="name">
                </div>

                <div class="form-group">
                    <label>نام مستعار</label>
                    <input type="text" class="form-control" name="slug">
                </div>
                <div class="form-group">
                    <label>توضیحات</label>
                    <textarea class="form-control" name="description"></textarea>
                </div>
                <div class="form-group">
                    <label>متا-توضیحات</label>
                    <textarea class="form-control" name="meta_desc"></textarea>
                </div>

                <div class="form-group">
                    <label>متا-کلمات</label>
                    <textarea class="form-control" name="meta_key"></textarea>
                </div>
                <div class="form-group">
                    <button type="submit" name="submit" class="btn btn-primary">ایجاد</button>
                </div>
            </form></div>
@endsection
